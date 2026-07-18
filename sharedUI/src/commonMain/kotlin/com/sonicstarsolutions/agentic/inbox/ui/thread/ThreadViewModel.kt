package com.sonicstarsolutions.agentic.inbox.ui.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailAttachment
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DownloadAttachmentUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetFoldersUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetThreadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.MarkThreadReadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.MoveEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SendDraftEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SetEmailReadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SetEmailStarredUseCase
import com.sonicstarsolutions.agentic.inbox.platform.AttachmentOpener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ThreadActionResult {
    data class Success(val message: String, val shouldNavigateBack: Boolean) : ThreadActionResult
    data class Failure(val message: String) : ThreadActionResult
}

data class ThreadUiState(
    val loading: Boolean = true,
    val messages: List<EmailDetail> = emptyList(),
    val errorMessage: String? = null,
    val expandedMessageId: String? = null,
    val imagesAllowedFor: Set<String> = emptySet(),
    /** Attachments currently being fetched — drives the per-chip progress spinner. */
    val downloadingAttachmentIds: Set<String> = emptySet(),
    val folders: List<Folder> = emptyList(),
    val actionInProgress: Boolean = false,
    val actionResult: ThreadActionResult? = null,
    /** The signed-in mailbox's own address, for showing "You" instead of the raw address when a
     * message's sender/recipient is this mailbox. Null until loaded, or if the lookup fails —
     * that failure is silent since it only degrades a display nicety, not core thread loading. */
    val mailboxEmail: String? = null,
)

class ThreadViewModel(
    private val getThread: GetThreadUseCase,
    private val getFolders: GetFoldersUseCase,
    private val moveEmail: MoveEmailUseCase,
    private val deleteEmail: DeleteEmailUseCase,
    private val setEmailRead: SetEmailReadUseCase,
    private val setEmailStarred: SetEmailStarredUseCase,
    private val markThreadRead: MarkThreadReadUseCase,
    private val sendDraftEmail: SendDraftEmailUseCase,
    private val getMailbox: GetMailboxUseCase,
    private val downloadAttachment: DownloadAttachmentUseCase,
    private val attachmentOpener: AttachmentOpener,
    private val mailboxId: String,
    private val emailId: String,
    private val threadId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(ThreadUiState())
    val state: StateFlow<ThreadUiState> = _state.asStateFlow()

    init {
        loadThread()
        loadFolders()
        loadMailboxEmail()
    }

    private fun loadMailboxEmail() {
        viewModelScope.launch {
            getMailbox(mailboxId).onSuccess { mailbox -> _state.update { it.copy(mailboxEmail = mailbox.email) } }
        }
    }

    /** Re-runs the initial thread load after a failure — the error screen's Retry button. */
    fun retry() {
        _state.update { it.copy(loading = true, errorMessage = null) }
        loadThread()
    }

    private fun loadThread() {
        viewModelScope.launch {
            getThread(mailboxId, emailId, threadId)
                .onSuccess { messages ->
                    _state.update {
                        it.copy(loading = false, messages = messages, expandedMessageId = messages.lastOrNull()?.id)
                    }
                    markReadIfNeeded(messages)
                }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to load") }
                }
        }
    }

    /** Opening a conversation marks it read, the way every mainstream mail client behaves —
     * silent (no Snackbar), and skipped entirely when there's nothing unread to begin with. */
    private suspend fun markReadIfNeeded(messages: List<EmailDetail>) {
        if (messages.none { !it.read }) return
        val result = if (threadId != null) markThreadRead(mailboxId, threadId) else setEmailRead(mailboxId, emailId, true)
        result.onSuccess {
            _state.update { current -> current.copy(messages = current.messages.map { it.copy(read = true) }) }
        }
    }

    private fun loadFolders() {
        viewModelScope.launch {
            // Failure is non-fatal here — the Move menu just stays empty; everything else works.
            getFolders(mailboxId).onSuccess { list -> _state.update { it.copy(folders = list) } }
        }
    }

    /** Single-expanded (accordion) — matches the mail-client convention of one open message at a time. */
    fun toggleExpanded(messageId: String) {
        _state.update { it.copy(expandedMessageId = if (it.expandedMessageId == messageId) null else messageId) }
    }

    fun allowImages(messageId: String) {
        _state.update { it.copy(imagesAllowedFor = it.imagesAllowedFor + messageId) }
    }

    fun consumeActionResult() {
        _state.update { it.copy(actionResult = null) }
    }

    fun archive() = moveTo(SystemFolders.ARCHIVE)

    fun moveTo(folderId: String) {
        val targetId = targetMessageId() ?: return
        val folderName = _state.value.folders.firstOrNull { it.id == folderId }?.name ?: folderId
        val message = if (folderId == SystemFolders.ARCHIVE) "Archived" else "Moved to $folderName"
        runAction {
            moveEmail(mailboxId, targetId, folderId).map { message }
        }
    }

    fun delete() {
        val targetId = targetMessageId() ?: return
        runAction {
            deleteEmail(mailboxId, targetId).map { "Deleted" }
        }
    }

    fun toggleReadState(messageId: String) {
        val target = _state.value.messages.firstOrNull { it.id == messageId } ?: return
        val newReadState = !target.read
        runAction(navigateBackOnSuccess = false) {
            setEmailRead(mailboxId, target.id, newReadState)
                .onSuccess {
                    _state.update { current ->
                        current.copy(messages = current.messages.map { if (it.id == target.id) it.copy(read = newReadState) else it })
                    }
                }
                .map { if (newReadState) "Marked as read" else "Marked as unread" }
        }
    }

    fun toggleStarred(messageId: String) {
        val target = _state.value.messages.firstOrNull { it.id == messageId } ?: return
        val newStarredState = !target.starred
        runAction(navigateBackOnSuccess = false) {
            setEmailStarred(mailboxId, target.id, newStarredState)
                .onSuccess {
                    _state.update { current ->
                        current.copy(messages = current.messages.map { if (it.id == target.id) it.copy(starred = newStarredState) else it })
                    }
                }
                .map { if (newStarredState) "Starred" else "Unstarred" }
        }
    }

    /**
     * Dispatches a draft message (a real row on the server sitting in the `draft` folder — see
     * [SendDraftEmailUseCase]) exactly as its fields currently read. Unlike the other actions in
     * this class this doesn't navigate back: the thread reloads instead, so the now-real sent
     * message takes the draft's place in the conversation the user is still looking at.
     */
    fun sendDraft(draftMessageId: String) {
        if (_state.value.actionInProgress) return
        val draft = _state.value.messages.firstOrNull { it.id == draftMessageId } ?: return
        _state.update { it.copy(actionInProgress = true) }
        viewModelScope.launch {
            sendDraftEmail(mailboxId, draft)
                .onSuccess {
                    _state.update {
                        it.copy(actionInProgress = false, actionResult = ThreadActionResult.Success("Sent", shouldNavigateBack = false))
                    }
                    loadThread()
                }
                .onFailure { t ->
                    val message = t.message ?: t::class.simpleName ?: "Failed to send"
                    _state.update { it.copy(actionInProgress = false, actionResult = ThreadActionResult.Failure(message)) }
                }
        }
    }

    fun openAttachment(messageId: String, attachmentId: String) =
        withDownloadedAttachment(messageId, attachmentId) { path, attachment ->
            attachmentOpener.open(path, attachment.mimetype)
        }

    fun shareAttachment(messageId: String, attachmentId: String) =
        withDownloadedAttachment(messageId, attachmentId) { path, attachment ->
            attachmentOpener.share(path, attachment.mimetype)
        }

    /** Downloads (cache-first) then hands the local file to [action]. One in-flight download per
     * attachment — repeat taps while the spinner shows are swallowed, not queued. */
    private fun withDownloadedAttachment(
        messageId: String,
        attachmentId: String,
        action: (path: String, attachment: EmailAttachment) -> Result<Unit>,
    ) {
        val message = _state.value.messages.firstOrNull { it.id == messageId } ?: return
        val attachment = message.attachments.firstOrNull { it.id == attachmentId } ?: return
        if (attachmentId in _state.value.downloadingAttachmentIds) return
        _state.update { it.copy(downloadingAttachmentIds = it.downloadingAttachmentIds + attachmentId) }
        viewModelScope.launch {
            downloadAttachment(mailboxId, message.id, attachment)
                .mapCatching { path -> action(path, attachment).getOrThrow() }
                .onFailure { t ->
                    val text = t.message ?: t::class.simpleName ?: "Couldn't open attachment"
                    _state.update { it.copy(actionResult = ThreadActionResult.Failure(text)) }
                }
            _state.update { it.copy(downloadingAttachmentIds = it.downloadingAttachmentIds - attachmentId) }
        }
    }

    private fun targetMessageId(): String? = _state.value.expandedMessageId ?: _state.value.messages.firstOrNull()?.id ?: emailId

    private fun runAction(navigateBackOnSuccess: Boolean = true, action: suspend () -> Result<String>) {
        if (_state.value.actionInProgress) return
        _state.update { it.copy(actionInProgress = true) }
        viewModelScope.launch {
            action()
                .onSuccess { message ->
                    _state.update {
                        it.copy(actionInProgress = false, actionResult = ThreadActionResult.Success(message, navigateBackOnSuccess))
                    }
                }
                .onFailure { t ->
                    val message = t.message ?: t::class.simpleName ?: "Something went wrong"
                    _state.update { it.copy(actionInProgress = false, actionResult = ThreadActionResult.Failure(message)) }
                }
        }
    }
}
