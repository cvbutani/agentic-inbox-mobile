package com.sonicstarsolutions.agentic.inbox.testutil

import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.model.SearchQuery
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
import com.sonicstarsolutions.agentic.inbox.domain.repository.ConnectionRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.CredentialsRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.FolderRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.ThreadRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory test double: separates "persisted" storage from the observable [state], the way the
 * real KSafe-backed repository does (state only reflects storage after [loadIntoState]). */
class FakeCredentialsRepository(
    var stored: Credentials = Credentials(),
) : CredentialsRepository {
    private val _state = MutableStateFlow(Credentials())
    override val state: StateFlow<Credentials> = _state.asStateFlow()

    override suspend fun save(credentials: Credentials) {
        stored = credentials
        _state.value = credentials
    }

    override suspend fun clear() {
        stored = Credentials()
        _state.value = Credentials()
    }

    override suspend fun loadIntoState() {
        _state.value = stored
    }
}

class FakeConnectionRepository(
    var result: Result<Unit> = Result.success(Unit),
) : ConnectionRepository {
    override suspend fun validate(): Result<Unit> = result
}

class FakeFolderRepository(
    var result: Result<List<Folder>> = Result.success(SystemFolders.defaults),
    var createResult: (String) -> Result<Folder> = { name -> Result.success(Folder(id = name.lowercase(), name = name, isSystem = false)) },
    var renameResult: (String, String) -> Result<Folder> = { folderId, name -> Result.success(Folder(id = folderId, name = name, isSystem = false)) },
    var deleteResult: Result<Unit> = Result.success(Unit),
) : FolderRepository {
    val createCalls = mutableListOf<Pair<String, String>>()
    val renameCalls = mutableListOf<Triple<String, String, String>>()
    val deleteCalls = mutableListOf<Pair<String, String>>()

    /** When set, [createFolder]/[renameFolder]/[deleteFolder] suspend here until completed —
     * lets tests hold a call "in flight" to exercise concurrency guards on InboxViewModel. */
    var createGate: CompletableDeferred<Unit>? = null
    var renameGate: CompletableDeferred<Unit>? = null
    var deleteGate: CompletableDeferred<Unit>? = null

    override suspend fun getFolders(mailboxId: String): Result<List<Folder>> = result

    override suspend fun createFolder(mailboxId: String, name: String): Result<Folder> {
        createCalls += mailboxId to name
        createGate?.await()
        return createResult(name)
    }

    override suspend fun renameFolder(mailboxId: String, folderId: String, name: String): Result<Folder> {
        renameCalls += Triple(mailboxId, folderId, name)
        renameGate?.await()
        return renameResult(folderId, name)
    }

    override suspend fun deleteFolder(mailboxId: String, folderId: String): Result<Unit> {
        deleteCalls += mailboxId to folderId
        deleteGate?.await()
        return deleteResult
    }
}

class FakeMailboxRepository(
    var result: Result<List<Mailbox>> = Result.success(emptyList()),
    var createResult: (String, String) -> Result<Mailbox> =
        { email, name -> Result.success(Mailbox(id = email.substringBefore("@"), email = email, name = name)) },
    var allowedDomainsResult: Result<List<String>> = Result.success(emptyList()),
) : MailboxRepository {
    data class CreateCall(val email: String, val name: String)

    val createCalls = mutableListOf<CreateCall>()

    /** When set, [createMailbox] suspends here until completed — lets tests hold a call "in
     * flight" to exercise concurrency guards (e.g. MailboxPickerViewModel.createMailbox). */
    var createGate: CompletableDeferred<Unit>? = null

    override suspend fun getMailboxes(): Result<List<Mailbox>> = result

    override suspend fun createMailbox(email: String, name: String): Result<Mailbox> {
        createCalls += CreateCall(email, name)
        createGate?.await()
        return createResult(email, name)
    }

    override suspend fun getAllowedDomains(): Result<List<String>> = allowedDomainsResult

    var getMailboxResult: (String) -> Result<Mailbox> =
        { id -> Result.success(Mailbox(id = id, email = "$id@example.dev", name = "Mailbox $id")) }

    override suspend fun getMailbox(mailboxId: String): Result<Mailbox> = getMailboxResult(mailboxId)
}

class FakeEmailRepository(
    var handler: (mailboxId: String, folder: String, page: Int, limit: Int) -> Result<EmailPage> =
        { _, _, _, _ -> Result.success(EmailPage(emptyList(), 0)) },
    var searchHandler: (mailboxId: String, query: SearchQuery, page: Int, limit: Int) -> Result<EmailPage> =
        { _, _, _, _ -> Result.success(EmailPage(emptyList(), 0)) },
    var moveResult: Result<Unit> = Result.success(Unit),
    var deleteResult: Result<Unit> = Result.success(Unit),
    var setReadResult: Result<Unit> = Result.success(Unit),
    var markThreadReadResult: Result<Unit> = Result.success(Unit),
    var sendResult: Result<Unit> = Result.success(Unit),
    var replyResult: Result<Unit> = Result.success(Unit),
    var forwardResult: Result<Unit> = Result.success(Unit),
) : EmailRepository {
    data class Call(val mailboxId: String, val folder: String, val page: Int, val limit: Int)
    data class SearchCall(val mailboxId: String, val query: SearchQuery, val page: Int, val limit: Int)
    data class MoveCall(val mailboxId: String, val emailId: String, val folderId: String)
    data class DeleteCall(val mailboxId: String, val emailId: String)
    data class SetReadCall(val mailboxId: String, val emailId: String, val read: Boolean)
    data class MarkThreadReadCall(val mailboxId: String, val threadId: String)
    data class SendCall(val mailboxId: String, val request: ComposeEmailRequest)
    data class ReplyCall(val mailboxId: String, val emailId: String, val request: ComposeEmailRequest)
    data class ForwardCall(val mailboxId: String, val emailId: String, val request: ComposeEmailRequest)

    val calls = mutableListOf<Call>()
    val searchCalls = mutableListOf<SearchCall>()
    val moveCalls = mutableListOf<MoveCall>()
    val deleteCalls = mutableListOf<DeleteCall>()
    val setReadCalls = mutableListOf<SetReadCall>()
    val markThreadReadCalls = mutableListOf<MarkThreadReadCall>()
    val sendCalls = mutableListOf<SendCall>()
    val replyCalls = mutableListOf<ReplyCall>()
    val forwardCalls = mutableListOf<ForwardCall>()

    /** When set, [sendEmail]/[replyEmail]/[forwardEmail] suspend here until completed — lets
     * tests hold a send "in flight" to exercise ComposeViewModel's concurrency guard. */
    var sendGate: CompletableDeferred<Unit>? = null

    /** When set, [getEmails] suspends here until completed — lets tests hold a call "in flight"
     * to exercise concurrency guards (e.g. InboxViewModel.onRefresh ignoring a second call). */
    var gate: CompletableDeferred<Unit>? = null

    /** Same idea as [gate], for [moveEmail] — e.g. ThreadViewModel.archive() ignoring a second tap. */
    var moveGate: CompletableDeferred<Unit>? = null

    /** Same idea as [gate], for [search] — e.g. SearchViewModel.search() ignoring a second tap. */
    var searchGate: CompletableDeferred<Unit>? = null

    override suspend fun getEmails(mailboxId: String, folder: String, page: Int, limit: Int): Result<EmailPage> {
        calls += Call(mailboxId, folder, page, limit)
        gate?.await()
        return handler(mailboxId, folder, page, limit)
    }

    override suspend fun search(mailboxId: String, query: SearchQuery, page: Int, limit: Int): Result<EmailPage> {
        searchCalls += SearchCall(mailboxId, query, page, limit)
        searchGate?.await()
        return searchHandler(mailboxId, query, page, limit)
    }

    override suspend fun moveEmail(mailboxId: String, emailId: String, folderId: String): Result<Unit> {
        moveCalls += MoveCall(mailboxId, emailId, folderId)
        moveGate?.await()
        return moveResult
    }

    override suspend fun deleteEmail(mailboxId: String, emailId: String): Result<Unit> {
        deleteCalls += DeleteCall(mailboxId, emailId)
        return deleteResult
    }

    override suspend fun setRead(mailboxId: String, emailId: String, read: Boolean): Result<Unit> {
        setReadCalls += SetReadCall(mailboxId, emailId, read)
        return setReadResult
    }

    override suspend fun markThreadRead(mailboxId: String, threadId: String): Result<Unit> {
        markThreadReadCalls += MarkThreadReadCall(mailboxId, threadId)
        return markThreadReadResult
    }

    override suspend fun sendEmail(mailboxId: String, request: ComposeEmailRequest): Result<Unit> {
        sendCalls += SendCall(mailboxId, request)
        sendGate?.await()
        return sendResult
    }

    override suspend fun replyEmail(mailboxId: String, emailId: String, request: ComposeEmailRequest): Result<Unit> {
        replyCalls += ReplyCall(mailboxId, emailId, request)
        sendGate?.await()
        return replyResult
    }

    override suspend fun forwardEmail(mailboxId: String, emailId: String, request: ComposeEmailRequest): Result<Unit> {
        forwardCalls += ForwardCall(mailboxId, emailId, request)
        sendGate?.await()
        return forwardResult
    }
}

class FakeThreadRepository(
    var result: Result<List<EmailDetail>> = Result.success(emptyList()),
) : ThreadRepository {
    override suspend fun getThread(mailboxId: String, emailId: String, threadId: String?): Result<List<EmailDetail>> = result
}
