package com.sonicstarsolutions.agentic.inbox.testutil

import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
import com.sonicstarsolutions.agentic.inbox.domain.repository.ConnectionRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.CredentialsRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.FolderRepository
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository
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
) : FolderRepository {
    override suspend fun getFolders(mailboxId: String): Result<List<Folder>> = result
}

class FakeMailboxRepository(
    var result: Result<List<Mailbox>> = Result.success(emptyList()),
) : MailboxRepository {
    override suspend fun getMailboxes(): Result<List<Mailbox>> = result
}

class FakeEmailRepository(
    var handler: (mailboxId: String, folder: String, page: Int, limit: Int) -> Result<EmailPage> =
        { _, _, _, _ -> Result.success(EmailPage(emptyList(), 0)) },
    var moveResult: Result<Unit> = Result.success(Unit),
    var deleteResult: Result<Unit> = Result.success(Unit),
    var setReadResult: Result<Unit> = Result.success(Unit),
    var markThreadReadResult: Result<Unit> = Result.success(Unit),
) : EmailRepository {
    data class Call(val mailboxId: String, val folder: String, val page: Int, val limit: Int)
    data class MoveCall(val mailboxId: String, val emailId: String, val folderId: String)
    data class DeleteCall(val mailboxId: String, val emailId: String)
    data class SetReadCall(val mailboxId: String, val emailId: String, val read: Boolean)
    data class MarkThreadReadCall(val mailboxId: String, val threadId: String)

    val calls = mutableListOf<Call>()
    val moveCalls = mutableListOf<MoveCall>()
    val deleteCalls = mutableListOf<DeleteCall>()
    val setReadCalls = mutableListOf<SetReadCall>()
    val markThreadReadCalls = mutableListOf<MarkThreadReadCall>()

    /** When set, [getEmails] suspends here until completed — lets tests hold a call "in flight"
     * to exercise concurrency guards (e.g. InboxViewModel.onRefresh ignoring a second call). */
    var gate: CompletableDeferred<Unit>? = null

    /** Same idea as [gate], for [moveEmail] — e.g. ThreadViewModel.archive() ignoring a second tap. */
    var moveGate: CompletableDeferred<Unit>? = null

    override suspend fun getEmails(mailboxId: String, folder: String, page: Int, limit: Int): Result<EmailPage> {
        calls += Call(mailboxId, folder, page, limit)
        gate?.await()
        return handler(mailboxId, folder, page, limit)
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
}
