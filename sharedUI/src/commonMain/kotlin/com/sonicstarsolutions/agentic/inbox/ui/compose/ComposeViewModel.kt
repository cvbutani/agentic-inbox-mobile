package com.sonicstarsolutions.agentic.inbox.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.model.Draft
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ForwardEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetThreadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ReplyEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SaveDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SendEmailUseCase
import com.sonicstarsolutions.agentic.inbox.util.EmailAddressUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
    /** Non-null once this composer has a draft — either resumed from one or after its first
     * autosave. Drives whether the screen offers "Discard draft". */
    val draftId: String? = null,
    val discarded: Boolean = false,
)

@OptIn(ExperimentalUuidApi::class)
class ComposeViewModel(
    private val getMailbox: GetMailboxUseCase,
    private val getThread: GetThreadUseCase,
    private val sendEmailUseCase: SendEmailUseCase,
    private val replyEmailUseCase: ReplyEmailUseCase,
    private val forwardEmailUseCase: ForwardEmailUseCase,
    private val saveDraftUseCase: SaveDraftUseCase,
    private val getDraftUseCase: GetDraftUseCase,
    private val deleteDraftUseCase: DeleteDraftUseCase,
    /**
     * Outlives this ViewModel. Saving a draft happens exactly when the composer is going away, so
     * a [viewModelScope] job would be cancelled before it could write — the text the user typed
     * would be lost precisely in the case autosave exists to cover.
     */
    private val externalScope: CoroutineScope,
    private val mailboxId: String,
    private val mode: ComposeMode,
    private val emailId: String?,
    private val threadId: String?,
    private val draftId: String? = null,
    private val newDraftId: () -> String = { Uuid.random().toString() },
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : ViewModel() {

    private val _state = MutableStateFlow(ComposeUiState(draftId = draftId))
    val state: StateFlow<ComposeUiState> = _state.asStateFlow()

    private var fromEmail: String = ""
    private var fromName: String = ""
    private var autosaveJob: Job? = null

    /** Only user edits mark the composer dirty. Prefilled reply/forward text doesn't, so opening
     * a reply and backing straight out leaves no draft behind. */
    private var dirty: Boolean = false

    companion object {
        /** Quiet period after the last keystroke before an autosave fires. Long enough that
         * ordinary typing doesn't hit the database on every character. */
        const val AUTOSAVE_DEBOUNCE_MILLIS: Long = 2_000
    }

    init {
        viewModelScope.launch {
            getMailbox(mailboxId).onSuccess { mailbox ->
                fromEmail = mailbox.email
                fromName = mailbox.name
            }
            val resumed = draftId?.let { getDraftUseCase(it) }
            if (resumed != null) {
                _state.update {
                    it.copy(
                        loading = false,
                        to = resumed.to,
                        cc = resumed.cc,
                        bcc = resumed.bcc,
                        subject = resumed.subject,
                        body = resumed.body,
                        draftId = resumed.id,
                    )
                }
            } else {
                loadOriginalIfNeeded()
            }
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

    fun onToChanged(value: String) = edit { it.copy(to = value, errorMessage = null) }
    fun onCcChanged(value: String) = edit { it.copy(cc = value) }
    fun onBccChanged(value: String) = edit { it.copy(bcc = value) }
    fun onSubjectChanged(value: String) = edit { it.copy(subject = value) }
    fun onBodyChanged(value: String) = edit { it.copy(body = value) }

    private fun edit(update: (ComposeUiState) -> ComposeUiState) {
        _state.update(update)
        dirty = true
        // The id is claimed here, on the first edit, rather than inside persistDraft: a debounced
        // autosave and a saveDraftNow racing each other would otherwise both see a null draftId
        // and mint one id each, leaving two rows in the Drafts folder for one message.
        // The id is claimed here, on the first edit, rather than inside persistDraft: a debounced
        // autosave and a saveDraftNow racing each other would otherwise both see a null draftId
        // and mint one id each, leaving two rows in the Drafts folder for one message.
        if (_state.value.draftId == null) {
            _state.update { it.copy(draftId = newDraftId()) }
        }
        scheduleAutosave()
    }

    private fun scheduleAutosave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DEBOUNCE_MILLIS)
            persistDraft()
        }
    }

    /** Saves right now instead of waiting out the debounce — for when the composer is closing and
     * the coroutine that owns the pending autosave is about to go away with it. */
    fun saveDraftNow() {
        autosaveJob?.cancel()
        autosaveJob = null
        externalScope.launch { persistDraft() }
    }

    private suspend fun persistDraft() {
        if (!dirty || _state.value.discarded || _state.value.sent) return
        val current = _state.value
        // Non-null in practice: edit() claims the id, and dirty is only set there.
        val id = current.draftId ?: return
        val draft = Draft(
            id = id,
            mailboxId = mailboxId,
            to = current.to,
            cc = current.cc,
            bcc = current.bcc,
            subject = current.subject,
            body = current.body,
            mode = mode.name,
            originalEmailId = emailId,
            threadId = threadId,
            updatedAt = now(),
        )
        if (draft.isEmpty) {
            // Everything's been cleared out — drop the draft rather than keep an empty row.
            current.draftId?.let { deleteDraftUseCase(it) }
            _state.update { it.copy(draftId = null) }
            return
        }
        saveDraftUseCase(draft)
        _state.update { it.copy(draftId = id) }
    }

    fun discardDraft() {
        autosaveJob?.cancel()
        autosaveJob = null
        val id = _state.value.draftId
        _state.update { it.copy(discarded = true) }
        // Also external: discarding navigates away, taking viewModelScope with it.
        externalScope.launch {
            id?.let { deleteDraftUseCase(it) }
        }
    }

    fun consumeError() = _state.update { it.copy(errorMessage = null) }

    fun send() {
        val toList = EmailAddressUtils.parseAddressList(_state.value.to)
        if (toList.isEmpty()) {
            _state.update { it.copy(errorMessage = "At least one recipient is required.") }
            return
        }
        if (_state.value.sending) return
        autosaveJob?.cancel()
        autosaveJob = null
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
                .onSuccess {
                    // The message is out; its draft has served its purpose. A send that failed
                    // keeps the draft, so nothing the user wrote is lost.
                    _state.value.draftId?.let { deleteDraftUseCase(it) }
                    _state.update { it.copy(sending = false, sent = true, draftId = null) }
                }
                .onFailure { t ->
                    _state.update { it.copy(sending = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to send") }
                }
        }
    }
}
