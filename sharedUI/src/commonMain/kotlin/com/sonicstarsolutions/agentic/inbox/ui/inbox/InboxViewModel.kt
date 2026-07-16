package com.sonicstarsolutions.agentic.inbox.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetEmailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InboxUiState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val emails: List<EmailSummary> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val errorMessage: String? = null,
    val currentMailboxName: String = "Inbox",
)

class InboxViewModel(
    private val getEmails: GetEmailsUseCase,
    private val mailboxId: String,
    private val mailboxName: String = "Inbox",
) : ViewModel() {

    private val _state = MutableStateFlow(InboxUiState(currentMailboxName = mailboxName))
    val state: StateFlow<InboxUiState> = _state.asStateFlow()

    init { loadFirstPage() }

    fun loadFirstPage() {
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            getEmails(mailboxId, folder = "inbox", page = 1, limit = 50)
                .onSuccess { page ->
                    _state.update { it.copy(loading = false, emails = page.emails, totalCount = page.totalCount, page = 1) }
                }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to load") }
                }
        }
    }

    fun onRefresh() {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true, errorMessage = null) }
        viewModelScope.launch {
            getEmails(mailboxId, folder = "inbox", page = 1, limit = 50)
                .onSuccess { page ->
                    _state.update { it.copy(refreshing = false, emails = page.emails, totalCount = page.totalCount, page = 1) }
                }
                .onFailure { t ->
                    _state.update { it.copy(refreshing = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to refresh") }
                }
        }
    }

    fun loadMore() {
        val currentState = _state.value
        if (currentState.loading || currentState.emails.size >= currentState.totalCount) return
        val nextPage = currentState.page + 1
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            getEmails(mailboxId, folder = "inbox", page = nextPage, limit = 50)
                .onSuccess { page ->
                    _state.update { it.copy(loading = false, emails = it.emails + page.emails, totalCount = page.totalCount, page = nextPage) }
                }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to load more") }
                }
        }
    }
}
