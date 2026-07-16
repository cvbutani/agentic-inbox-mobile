package com.sonicstarsolutions.agentic.inbox.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetFoldersUseCase
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
    val folders: List<Folder> = SystemFolders.defaults,
    val currentFolder: Folder = SystemFolders.defaults.first { it.id == SystemFolders.INBOX },
)

class InboxViewModel(
    private val getEmails: GetEmailsUseCase,
    private val getFolders: GetFoldersUseCase,
    private val mailboxId: String,
    private val mailboxName: String = "Inbox",
) : ViewModel() {

    private val _state = MutableStateFlow(InboxUiState(currentMailboxName = mailboxName))
    val state: StateFlow<InboxUiState> = _state.asStateFlow()

    init {
        loadFirstPage()
        loadFolders()
    }

    fun loadFirstPage() {
        val folderId = _state.value.currentFolder.id
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            getEmails(mailboxId, folder = folderId, page = 1, limit = 50)
                .onSuccess { page ->
                    _state.update { it.copy(loading = false, emails = page.emails, totalCount = page.totalCount, page = 1) }
                }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to load") }
                }
        }
    }

    private fun loadFolders() {
        viewModelScope.launch {
            // Failure is non-fatal: the initial state already seeds the system-folder defaults,
            // so the drawer still works — it just won't have unread counts or custom folders yet.
            getFolders(mailboxId).onSuccess { list ->
                _state.update { current ->
                    val stillSelected = list.firstOrNull { it.id == current.currentFolder.id } ?: current.currentFolder
                    current.copy(folders = list, currentFolder = stillSelected)
                }
            }
        }
    }

    fun onRefresh() {
        if (_state.value.refreshing) return
        val folderId = _state.value.currentFolder.id
        _state.update { it.copy(refreshing = true, errorMessage = null) }
        viewModelScope.launch {
            getEmails(mailboxId, folder = folderId, page = 1, limit = 50)
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
        val folderId = currentState.currentFolder.id
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            getEmails(mailboxId, folder = folderId, page = nextPage, limit = 50)
                .onSuccess { page ->
                    _state.update { it.copy(loading = false, emails = it.emails + page.emails, totalCount = page.totalCount, page = nextPage) }
                }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to load more") }
                }
        }
    }

    fun selectFolder(folder: Folder) {
        if (folder.id == _state.value.currentFolder.id) return
        _state.update {
            it.copy(
                currentFolder = folder,
                emails = emptyList(),
                totalCount = 0,
                page = 1,
                loading = true,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            getEmails(mailboxId, folder = folder.id, page = 1, limit = 50)
                .onSuccess { page ->
                    _state.update { it.copy(loading = false, emails = page.emails, totalCount = page.totalCount, page = 1) }
                }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to load") }
                }
        }
    }
}
