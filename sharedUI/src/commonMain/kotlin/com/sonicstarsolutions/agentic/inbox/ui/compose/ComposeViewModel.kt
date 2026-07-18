package com.sonicstarsolutions.agentic.inbox.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.model.Draft
import com.sonicstarsolutions.agentic.inbox.domain.model.displayName
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ForwardEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetThreadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ReplyEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SaveDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SendEmailUseCase
import com.sonicstarsolutions.agentic.inbox.util.EmailAddressUtils
import com.sonicstarsolutions.agentic.inbox.util.HtmlTextExtractor
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

enum class ComposeMode {
    NEW, REPLY, REPLY_ALL, FORWARD,

    /**
     * Resumes a draft that's a real row on the server (sitting in the `draft` folder — see
     * [com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail.inReplyTo]), reached from
     * ThreadScreen. Distinct from the app's own local-only Draft (Room) feature, which resumes via
     * [draftId] on the other modes instead.
     */
    EDIT_DRAFT,
}

data class ComposeUiState(
    val loading: Boolean = true,
    /** The sending mailbox's address, for the composer's read-only From row — in a
     * multi-mailbox app the sender identity shouldn't be a mystery. */
    val fromAddress: String = "",
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
    /** Server-side cleanup after an EDIT_DRAFT send — distinct from [deleteDraftUseCase], which
     * only ever touches this app's own local Room-backed drafts. */
    private val deleteEmailUseCase: DeleteEmailUseCase,
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

    /** Set while loading an EDIT_DRAFT message — the id of the message *that draft* was replying
     * to (not the draft's own id), which is what actually decides the send path. Null means this
     * was a from-scratch compose saved as a draft, so sending falls back to a plain send. */
    private var editDraftInReplyTo: String? = null

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
                fromName = mailbox.displayName
                _state.update { it.copy(fromAddress = mailbox.email) }
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
                        body = editableBody(resumed.body),
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
                        ComposeMode.EDIT_DRAFT -> {
                            // Continuing an existing draft, not deriving a fresh reply/forward — its
                            // own current fields are the whole message, not a starting point to
                            // quote or re-prefix. The body still converts to editable text: server
                            // drafts (the Worker's AI writes these) are HTML, and the composer is
                            // a plain TextField — raw markup would make the user edit angle brackets.
                            editDraftInReplyTo = original.inReplyTo
                            PrefilledFields(
                                to = original.recipient,
                                cc = original.cc.orEmpty(),
                                bcc = original.bcc.orEmpty(),
                                subject = original.subject,
                                body = editableBody(original.body.orEmpty()),
                            )
                        }
                    }
                    _state.update {
                        it.copy(
                            loading = false,
                            to = fields.to,
                            cc = fields.cc,
                            bcc = fields.bcc,
                            subject = fields.subject,
                            body = fields.body,
                        )
                    }
                }
            }
            .onFailure { t ->
                _state.update { it.copy(loading = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to load") }
            }
    }

    /** HTML bodies (server drafts, legacy autosaves of them) become editable plain text;
     * bodies that are already plain pass through byte-for-byte. */
    private fun editableBody(body: String): String =
        if (HtmlTextExtractor.containsHtml(body)) HtmlTextExtractor.toEditableText(body) else body

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
                // The draft's own inReplyTo (not emailId, which is the draft's own id) decides the
                // path — the server derives correct threading headers from whatever message that
                // id points to, matching workers/routes/reply-forward.ts in cloudflare/agentic-inbox.
                ComposeMode.EDIT_DRAFT -> editDraftInReplyTo
                    ?.let { originalEmailId -> replyEmailUseCase(mailboxId, originalEmailId, request) }
                    ?: sendEmailUseCase(mailboxId, request)
            }
            result
                .onSuccess {
                    // The message is out; its draft has served its purpose. A send that failed
                    // keeps the draft, so nothing the user wrote is lost.
                    _state.value.draftId?.let { deleteDraftUseCase(it) }
                    // EDIT_DRAFT's draft is a real row on the server (emailId is its id), separate
                    // from the local-only one above — best-effort, same reasoning as
                    // SendDraftEmailUseCase: the send already succeeded, so a cleanup failure here
                    // must not be reported as this send having failed.
                    if (mode == ComposeMode.EDIT_DRAFT) emailId?.let { deleteEmailUseCase(mailboxId, it) }
                    _state.update { it.copy(sending = false, sent = true, draftId = null) }
                }
                .onFailure { t ->
                    _state.update { it.copy(sending = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to send") }
                }
        }
    }
}
