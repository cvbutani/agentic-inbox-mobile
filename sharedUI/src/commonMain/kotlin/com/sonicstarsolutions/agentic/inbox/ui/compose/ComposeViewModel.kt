package com.sonicstarsolutions.agentic.inbox.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ForwardEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetThreadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ReplyEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SendEmailUseCase
import com.sonicstarsolutions.agentic.inbox.util.EmailAddressUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ComposeMode { NEW, REPLY, REPLY_ALL, FORWARD }

data class ComposeUiState(
    val loading: Boolean = true,
    val to: String = "",
    val cc: String = "",
    val bcc: String = "",
    val subject: String = "",
    val body: String = "",
    val sending: Boolean = false,
    val errorMessage: String? = null,
    val sent: Boolean = false,
)

class ComposeViewModel(
    private val getMailbox: GetMailboxUseCase,
    private val getThread: GetThreadUseCase,
    private val sendEmailUseCase: SendEmailUseCase,
    private val replyEmailUseCase: ReplyEmailUseCase,
    private val forwardEmailUseCase: ForwardEmailUseCase,
    private val mailboxId: String,
    private val mode: ComposeMode,
    private val emailId: String?,
    private val threadId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(ComposeUiState())
    val state: StateFlow<ComposeUiState> = _state.asStateFlow()

    private var fromEmail: String = ""
    private var fromName: String = ""

    init {
        viewModelScope.launch {
            getMailbox(mailboxId).onSuccess { mailbox ->
                fromEmail = mailbox.email
                fromName = mailbox.name
            }
            loadOriginalIfNeeded()
        }
    }

    private suspend fun loadOriginalIfNeeded() {
        if (mode == ComposeMode.NEW || emailId == null) {
            _state.update { it.copy(loading = false) }
            return
        }
        getThread(mailboxId, emailId, threadId)
            .onSuccess { messages ->
                // Reply/Forward/Reply-all all target the specific message the user acted from
                // (the one they had expanded in ThreadScreen) — falling back to the thread's
                // last message only if that id somehow isn't in the response.
                val original = messages.firstOrNull { it.id == emailId } ?: messages.lastOrNull()
                if (original == null) {
                    _state.update { it.copy(loading = false) }
                } else {
                    val fields = when (mode) {
                        ComposeMode.REPLY -> ComposePrefill.forReply(original, replyAll = false, ownEmail = fromEmail)
                        ComposeMode.REPLY_ALL -> ComposePrefill.forReply(original, replyAll = true, ownEmail = fromEmail)
                        ComposeMode.FORWARD -> ComposePrefill.forForward(original)
                        ComposeMode.NEW -> PrefilledFields(to = "", cc = "", subject = "", body = "")
                    }
                    _state.update {
                        it.copy(loading = false, to = fields.to, cc = fields.cc, subject = fields.subject, body = fields.body)
                    }
                }
            }
            .onFailure { t ->
                _state.update { it.copy(loading = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to load") }
            }
    }

    fun onToChanged(value: String) = _state.update { it.copy(to = value, errorMessage = null) }
    fun onCcChanged(value: String) = _state.update { it.copy(cc = value) }
    fun onBccChanged(value: String) = _state.update { it.copy(bcc = value) }
    fun onSubjectChanged(value: String) = _state.update { it.copy(subject = value) }
    fun onBodyChanged(value: String) = _state.update { it.copy(body = value) }

    fun consumeError() = _state.update { it.copy(errorMessage = null) }

    fun send() {
        val toList = EmailAddressUtils.parseAddressList(_state.value.to)
        if (toList.isEmpty()) {
            _state.update { it.copy(errorMessage = "At least one recipient is required.") }
            return
        }
        if (_state.value.sending) return
        _state.update { it.copy(sending = true, errorMessage = null) }
        viewModelScope.launch {
            val request = ComposeEmailRequest(
                fromEmail = fromEmail,
                fromName = fromName,
                to = toList,
                cc = EmailAddressUtils.parseAddressList(_state.value.cc),
                bcc = EmailAddressUtils.parseAddressList(_state.value.bcc),
                subject = _state.value.subject.trim(),
                body = _state.value.body,
            )
            val result = when (mode) {
                ComposeMode.NEW -> sendEmailUseCase(mailboxId, request)
                ComposeMode.REPLY, ComposeMode.REPLY_ALL -> replyEmailUseCase(mailboxId, emailId!!, request)
                ComposeMode.FORWARD -> forwardEmailUseCase(mailboxId, emailId!!, request)
            }
            result
                .onSuccess { _state.update { it.copy(sending = false, sent = true) } }
                .onFailure { t ->
                    _state.update { it.copy(sending = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to send") }
                }
        }
    }
}
