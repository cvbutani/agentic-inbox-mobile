package com.sonicstarsolutions.agentic.inbox.ui.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetThreadUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ThreadUiState(
    val loading: Boolean = true,
    val messages: List<EmailDetail> = emptyList(),
    val errorMessage: String? = null,
    val expandedMessageId: String? = null,
    val imagesAllowedFor: Set<String> = emptySet(),
)

class ThreadViewModel(
    private val getThread: GetThreadUseCase,
    private val mailboxId: String,
    private val emailId: String,
    private val threadId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(ThreadUiState())
    val state: StateFlow<ThreadUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getThread(mailboxId, emailId, threadId)
                .onSuccess { messages ->
                    _state.update {
                        it.copy(loading = false, messages = messages, expandedMessageId = messages.lastOrNull()?.id)
                    }
                }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to load") }
                }
        }
    }

    /** Single-expanded (accordion) — matches the mail-client convention of one open message at a time. */
    fun toggleExpanded(messageId: String) {
        _state.update { it.copy(expandedMessageId = if (it.expandedMessageId == messageId) null else messageId) }
    }

    fun allowImages(messageId: String) {
        _state.update { it.copy(imagesAllowedFor = it.imagesAllowedFor + messageId) }
    }
}
