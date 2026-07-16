package com.sonicstarsolutions.agentic.inbox.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CreateFolderUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteFolderUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetFoldersUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.RenameFolderUseCase
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
    val creatingFolder: Boolean = false,
    val folderActionError: String? = null,
    val folderCreated: Boolean = false,
    val renamingFolder: Boolean = false,
    val folderRenamed: Boolean = false,
    val deletingFolder: Boolean = false,
    val folderDeleted: Boolean = false,
)

class InboxViewModel(
    private val getEmails: GetEmailsUseCase,
    private val getFolders: GetFoldersUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val renameFolderUseCase: RenameFolderUseCase,
    private val deleteFolderUseCase: DeleteFolderUseCase,
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

    fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _state.update { it.copy(folderActionError = "Folder name is required.") }
            return
        }
        if (_state.value.creatingFolder) return
        _state.update { it.copy(creatingFolder = true, folderActionError = null) }
        viewModelScope.launch {
            createFolderUseCase(mailboxId, trimmed)
                .onSuccess { folder ->
                    _state.update { it.copy(creatingFolder = false, folders = it.folders + folder, folderCreated = true) }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(creatingFolder = false, folderActionError = t.message ?: t::class.simpleName ?: "Failed to create folder")
                    }
                }
        }
    }

    fun consumeFolderCreated() {
        _state.update { it.copy(folderCreated = false) }
    }

    fun consumeFolderActionError() {
        _state.update { it.copy(folderActionError = null) }
    }

    fun renameFolder(folder: Folder, name: String) {
        if (folder.isSystem) {
            _state.update { it.copy(folderActionError = "System folders can't be renamed.") }
            return
        }
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            _state.update { it.copy(folderActionError = "Folder name is required.") }
            return
        }
        if (_state.value.renamingFolder) return
        _state.update { it.copy(renamingFolder = true, folderActionError = null) }
        viewModelScope.launch {
            renameFolderUseCase(mailboxId, folder.id, trimmed)
                .onSuccess { renamed ->
                    // Only the name changed server-side — keep the locally known unreadCount
                    // rather than trusting the rename response for fields it isn't authoritative on.
                    val updated = folder.copy(name = renamed.name)
                    _state.update { current ->
                        current.copy(
                            renamingFolder = false,
                            folders = current.folders.map { f -> if (f.id == folder.id) updated else f },
                            currentFolder = if (current.currentFolder.id == folder.id) updated else current.currentFolder,
                            folderRenamed = true,
                        )
                    }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(renamingFolder = false, folderActionError = t.message ?: t::class.simpleName ?: "Failed to rename folder")
                    }
                }
        }
    }

    fun consumeFolderRenamed() {
        _state.update { it.copy(folderRenamed = false) }
    }

    fun deleteFolder(folder: Folder) {
        if (folder.isSystem) {
            _state.update { it.copy(folderActionError = "System folders can't be deleted.") }
            return
        }
        if (_state.value.deletingFolder) return
        _state.update { it.copy(deletingFolder = true, folderActionError = null) }
        viewModelScope.launch {
            deleteFolderUseCase(mailboxId, folder.id)
                .onSuccess {
                    _state.update {
                        it.copy(deletingFolder = false, folders = it.folders.filterNot { f -> f.id == folder.id }, folderDeleted = true)
                    }
                    if (_state.value.currentFolder.id == folder.id) {
                        val inbox = SystemFolders.defaults.first { it.id == SystemFolders.INBOX }
                        selectFolder(inbox)
                    }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(deletingFolder = false, folderActionError = t.message ?: t::class.simpleName ?: "Failed to delete folder")
                    }
                }
        }
    }

    fun consumeFolderDeleted() {
        _state.update { it.copy(folderDeleted = false) }
    }
}
