package com.sonicstarsolutions.agentic.inbox.ui.thread

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetFoldersUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetThreadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.MarkThreadReadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.MoveEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SendDraftEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SetEmailReadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SetEmailStarredUseCase
import com.sonicstarsolutions.agentic.inbox.testutil.FakeEmailRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeFolderRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeMailboxRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeThreadRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun detail(id: String, read: Boolean = true, starred: Boolean = false) = EmailDetail(
        id = id,
        subject = "Subject",
        sender = "a@example.dev",
        recipient = "b@example.dev",
        cc = null,
        bcc = null,
        date = "2026-07-16T00:00:00Z",
        read = read,
        starred = starred,
        threadId = "t1",
        folderId = "inbox",
        body = "<p>Body $id</p>",
        attachments = emptyList(),
    )

    private fun buildViewModel(
        threadRepository: FakeThreadRepository,
        emailRepository: FakeEmailRepository = FakeEmailRepository(),
        folderRepository: FakeFolderRepository = FakeFolderRepository(),
        mailboxRepository: FakeMailboxRepository = FakeMailboxRepository().apply {
            getMailboxResult = { Result.success(Mailbox(id = "mb1", email = "me@example.dev", name = "Me")) }
        },
        threadId: String? = "t1",
    ): ThreadViewModel = ThreadViewModel(
        getThread = GetThreadUseCase(threadRepository),
        getFolders = GetFoldersUseCase(folderRepository),
        moveEmail = MoveEmailUseCase(emailRepository),
        deleteEmail = DeleteEmailUseCase(emailRepository),
        setEmailRead = SetEmailReadUseCase(emailRepository),
        setEmailStarred = SetEmailStarredUseCase(emailRepository),
        markThreadRead = MarkThreadReadUseCase(emailRepository),
        sendDraftEmail = SendDraftEmailUseCase(emailRepository, mailboxRepository),
        getMailbox = GetMailboxUseCase(mailboxRepository),
        mailboxId = "mb1",
        emailId = "e1",
        threadId = threadId,
    )

    private fun draftDetail(id: String, inReplyTo: String? = null) = EmailDetail(
        id = id,
        subject = "Re: Subject",
        sender = "me@example.dev",
        recipient = "a@example.dev",
        cc = null,
        bcc = null,
        date = "2026-07-16T00:00:00Z",
        read = true,
        starred = false,
        threadId = "t1",
        folderId = SystemFolders.DRAFT,
        body = "<p>Draft body</p>",
        attachments = emptyList(),
        inReplyTo = inReplyTo,
    )

    @Test
    fun `loads messages and expands the latest one by default`() = runTest {
        val repository = FakeThreadRepository(
            result = Result.success(listOf(detail("e1"), detail("e2"), detail("e3"))),
        )

        val viewModel = buildViewModel(repository)

        assertFalse(viewModel.state.value.loading)
        assertEquals(listOf(detail("e1"), detail("e2"), detail("e3")), viewModel.state.value.messages)
        assertEquals("e3", viewModel.state.value.expandedMessageId)
    }

    @Test
    fun `loads the current mailbox's own email for the You substitution`() = runTest {
        val repository = FakeThreadRepository(result = Result.success(listOf(detail("e1"))))
        val mailboxRepository = FakeMailboxRepository().apply {
            getMailboxResult = { Result.success(Mailbox(id = "mb1", email = "me@example.dev", name = "Me")) }
        }

        val viewModel = buildViewModel(repository, mailboxRepository = mailboxRepository)

        assertEquals("me@example.dev", viewModel.state.value.mailboxEmail)
    }

    @Test
    fun `a failed mailbox lookup leaves mailboxEmail null without surfacing an error`() = runTest {
        val repository = FakeThreadRepository(result = Result.success(listOf(detail("e1"))))
        val mailboxRepository = FakeMailboxRepository().apply {
            getMailboxResult = { Result.failure(RuntimeException("offline")) }
        }

        val viewModel = buildViewModel(repository, mailboxRepository = mailboxRepository)

        assertNull(viewModel.state.value.mailboxEmail)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `failure surfaces an error message`() = runTest {
        val repository = FakeThreadRepository(result = Result.failure(RuntimeException("not found")))

        val viewModel = buildViewModel(repository)

        assertFalse(viewModel.state.value.loading)
        assertEquals("not found", viewModel.state.value.errorMessage)
        assertTrue(viewModel.state.value.messages.isEmpty())
    }

    @Test
    fun `retry after a failed load clears the error and loads the thread`() = runTest {
        val repository = FakeThreadRepository(result = Result.failure(RuntimeException("network down")))
        val viewModel = buildViewModel(repository)
        assertEquals("network down", viewModel.state.value.errorMessage)

        repository.result = Result.success(listOf(detail("e1"), detail("e2")))
        viewModel.retry()

        assertNull(viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.loading)
        assertEquals(listOf(detail("e1"), detail("e2")), viewModel.state.value.messages)
        assertEquals(2, repository.calls.size, "retry must issue a second thread request")
    }

    @Test
    fun `toggling the already-expanded message collapses it`() = runTest {
        val repository = FakeThreadRepository(result = Result.success(listOf(detail("e1"), detail("e2"))))
        val viewModel = buildViewModel(repository)
        assertEquals("e2", viewModel.state.value.expandedMessageId)

        viewModel.toggleExpanded("e2")

        assertNull(viewModel.state.value.expandedMessageId)
    }

    @Test
    fun `expanding a different message collapses the previous one`() = runTest {
        val repository = FakeThreadRepository(result = Result.success(listOf(detail("e1"), detail("e2"))))
        val viewModel = buildViewModel(repository)
        assertEquals("e2", viewModel.state.value.expandedMessageId)

        viewModel.toggleExpanded("e1")

        assertEquals("e1", viewModel.state.value.expandedMessageId)
    }

    @Test
    fun `allowImages marks the message as opted in for remote images`() = runTest {
        val repository = FakeThreadRepository(result = Result.success(listOf(detail("e1"))))
        val viewModel = buildViewModel(repository)
        assertFalse(viewModel.state.value.imagesAllowedFor.contains("e1"))

        viewModel.allowImages("e1")

        assertTrue(viewModel.state.value.imagesAllowedFor.contains("e1"))
    }

    @Test
    fun `folders load into state on init`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(detail("e1"))))
        val folderRepository = FakeFolderRepository(result = Result.success(SystemFolders.defaults))

        val viewModel = buildViewModel(threadRepository, folderRepository = folderRepository)

        assertEquals(SystemFolders.defaults, viewModel.state.value.folders)
    }

    @Test
    fun `archive moves the expanded message to Archive and requests navigating back`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(detail("e1"), detail("e2"))))
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository, emailRepository) // expanded = e2

        viewModel.archive()

        assertEquals(
            listOf(FakeEmailRepository.MoveCall("mb1", "e2", SystemFolders.ARCHIVE)),
            emailRepository.moveCalls,
        )
        val result = viewModel.state.value.actionResult
        assertTrue(result is ThreadActionResult.Success)
        assertTrue(result.shouldNavigateBack)
        assertFalse(viewModel.state.value.actionInProgress)
    }

    @Test
    fun `archive failure reports a failure result without navigating back`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(detail("e1"))))
        val emailRepository = FakeEmailRepository(moveResult = Result.failure(RuntimeException("offline")))
        val viewModel = buildViewModel(threadRepository, emailRepository)

        viewModel.archive()

        val result = viewModel.state.value.actionResult
        assertTrue(result is ThreadActionResult.Failure)
        assertEquals("offline", result.message)
    }

    @Test
    fun `delete removes the expanded message and requests navigating back`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(detail("e1"))))
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository, emailRepository)

        viewModel.delete()

        assertEquals(listOf(FakeEmailRepository.DeleteCall("mb1", "e1")), emailRepository.deleteCalls)
        val result = viewModel.state.value.actionResult
        assertTrue(result is ThreadActionResult.Success)
        assertTrue(result.shouldNavigateBack)
    }

    @Test
    fun `moveTo moves the expanded message to the chosen folder and requests navigating back`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(detail("e1"))))
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository, emailRepository)

        viewModel.moveTo("work")

        assertEquals(listOf(FakeEmailRepository.MoveCall("mb1", "e1", "work")), emailRepository.moveCalls)
        val result = viewModel.state.value.actionResult
        assertTrue(result is ThreadActionResult.Success)
        assertTrue(result.shouldNavigateBack)
    }

    @Test
    fun `toggleReadState flips the expanded message and does not navigate back`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(detail("e1", read = true))))
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository, emailRepository)

        viewModel.toggleReadState("e1")

        assertEquals(listOf(FakeEmailRepository.SetReadCall("mb1", "e1", false)), emailRepository.setReadCalls)
        assertFalse(viewModel.state.value.messages.single().read)
        val result = viewModel.state.value.actionResult
        assertTrue(result is ThreadActionResult.Success)
        assertFalse(result.shouldNavigateBack)
    }

    @Test
    fun `toggleStarred flips the expanded message and does not navigate back`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(detail("e1", starred = false))))
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository, emailRepository)

        viewModel.toggleStarred("e1")

        assertEquals(listOf(FakeEmailRepository.SetStarredCall("mb1", "e1", true)), emailRepository.setStarredCalls)
        assertTrue(viewModel.state.value.messages.single().starred)
        val result = viewModel.state.value.actionResult
        assertTrue(result is ThreadActionResult.Success)
        assertFalse(result.shouldNavigateBack)
    }

    @Test
    fun `toggleReadState can target a message other than the expanded one`() = runTest {
        val threadRepository = FakeThreadRepository(
            result = Result.success(listOf(detail("e1", read = true), detail("e2", read = true))),
        )
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository, emailRepository) // expanded = e2

        viewModel.toggleReadState("e1")

        assertEquals(listOf(FakeEmailRepository.SetReadCall("mb1", "e1", false)), emailRepository.setReadCalls)
        assertFalse(viewModel.state.value.messages.first { it.id == "e1" }.read)
        assertTrue(viewModel.state.value.messages.first { it.id == "e2" }.read, "the other message must stay untouched")
    }

    @Test
    fun `toggleStarred can target a message other than the expanded one`() = runTest {
        val threadRepository = FakeThreadRepository(
            result = Result.success(listOf(detail("e1", starred = false), detail("e2", starred = false))),
        )
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository, emailRepository) // expanded = e2

        viewModel.toggleStarred("e1")

        assertEquals(listOf(FakeEmailRepository.SetStarredCall("mb1", "e1", true)), emailRepository.setStarredCalls)
        assertTrue(viewModel.state.value.messages.first { it.id == "e1" }.starred)
        assertFalse(viewModel.state.value.messages.first { it.id == "e2" }.starred, "the other message must stay untouched")
    }

    @Test
    fun `toggleStarred failure reports a failure result and leaves the message unchanged`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(detail("e1", starred = false))))
        val emailRepository = FakeEmailRepository(setStarredResult = Result.failure(RuntimeException("offline")))
        val viewModel = buildViewModel(threadRepository, emailRepository)

        viewModel.toggleStarred("e1")

        val result = viewModel.state.value.actionResult
        assertTrue(result is ThreadActionResult.Failure)
        assertFalse(viewModel.state.value.messages.single().starred)
    }

    @Test
    fun `consumeActionResult clears the pending result`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(detail("e1"))))
        val viewModel = buildViewModel(threadRepository)
        viewModel.archive()
        assertTrue(viewModel.state.value.actionResult != null)

        viewModel.consumeActionResult()

        assertNull(viewModel.state.value.actionResult)
    }

    @Test
    fun `a second action is ignored while one is already in flight`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(detail("e1"))))
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository, emailRepository)
        val gate = CompletableDeferred<Unit>()
        emailRepository.moveGate = gate

        viewModel.archive()
        assertTrue(viewModel.state.value.actionInProgress)

        viewModel.delete() // should be ignored by the actionInProgress guard

        assertTrue(emailRepository.deleteCalls.isEmpty(), "delete should not have dispatched while archive was in flight")

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.actionInProgress)
    }

    @Test
    fun `opening a thread with unread messages silently marks the whole thread read`() = runTest {
        val threadRepository = FakeThreadRepository(
            result = Result.success(listOf(detail("e1", read = false), detail("e2", read = true))),
        )
        val emailRepository = FakeEmailRepository()

        val viewModel = buildViewModel(threadRepository, emailRepository, threadId = "t1")

        assertEquals(listOf(FakeEmailRepository.MarkThreadReadCall("mb1", "t1")), emailRepository.markThreadReadCalls)
        assertTrue(viewModel.state.value.messages.all { it.read })
        assertNull(viewModel.state.value.actionResult, "auto mark-as-read should be silent, no snackbar")
    }

    @Test
    fun `opening a thread that is already fully read does not call markThreadRead again`() = runTest {
        val threadRepository = FakeThreadRepository(
            result = Result.success(listOf(detail("e1", read = true), detail("e2", read = true))),
        )
        val emailRepository = FakeEmailRepository()

        buildViewModel(threadRepository, emailRepository, threadId = "t1")

        assertTrue(emailRepository.markThreadReadCalls.isEmpty())
    }

    @Test
    fun `opening a single unread non-threaded email marks just that email read`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(detail("e1", read = false))))
        val emailRepository = FakeEmailRepository()

        val viewModel = buildViewModel(threadRepository, emailRepository, threadId = null)

        assertEquals(listOf(FakeEmailRepository.SetReadCall("mb1", "e1", true)), emailRepository.setReadCalls)
        assertTrue(viewModel.state.value.messages.single().read)
        assertTrue(emailRepository.markThreadReadCalls.isEmpty())
    }

    @Test
    fun `opening an already-read non-threaded email does not call setRead again`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(detail("e1", read = true))))
        val emailRepository = FakeEmailRepository()

        buildViewModel(threadRepository, emailRepository, threadId = null)

        assertTrue(emailRepository.setReadCalls.isEmpty())
    }

    @Test
    fun `sendDraft with no inReplyTo sends via plain sendEmail and reports Sent without navigating back`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(draftDetail("d1"))))
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository, emailRepository)

        viewModel.sendDraft("d1")

        assertEquals(1, emailRepository.sendCalls.size)
        val result = viewModel.state.value.actionResult
        assertTrue(result is ThreadActionResult.Success)
        assertEquals("Sent", result.message)
        assertFalse(result.shouldNavigateBack)
        assertFalse(viewModel.state.value.actionInProgress)
    }

    @Test
    fun `sendDraft with inReplyTo sends via replyEmail targeting the original message`() = runTest {
        val threadRepository = FakeThreadRepository(
            result = Result.success(listOf(draftDetail("d1", inReplyTo = "original1"))),
        )
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository, emailRepository)

        viewModel.sendDraft("d1")

        assertEquals(listOf("original1"), emailRepository.replyCalls.map { it.emailId })
        assertTrue(emailRepository.sendCalls.isEmpty())
    }

    @Test
    fun `a successful sendDraft deletes the draft row and reloads the thread`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(draftDetail("d1"))))
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository, emailRepository)
        val callsBeforeSend = threadRepository.calls.size

        viewModel.sendDraft("d1")

        assertEquals(listOf(FakeEmailRepository.DeleteCall("mb1", "d1")), emailRepository.deleteCalls)
        assertTrue(threadRepository.calls.size > callsBeforeSend, "the thread should reload so the sent draft's real outcome shows")
    }

    @Test
    fun `sendDraft failure reports a Failure result and does not delete the draft`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(draftDetail("d1"))))
        val emailRepository = FakeEmailRepository(sendResult = Result.failure(RuntimeException("offline")))
        val viewModel = buildViewModel(threadRepository, emailRepository)

        viewModel.sendDraft("d1")

        val result = viewModel.state.value.actionResult
        assertTrue(result is ThreadActionResult.Failure)
        assertEquals("offline", result.message)
        assertTrue(emailRepository.deleteCalls.isEmpty())
    }

    @Test
    fun `sendDraft does nothing for a message id that is not in the thread`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(draftDetail("d1"))))
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository, emailRepository)

        viewModel.sendDraft("not-in-thread")

        assertTrue(emailRepository.sendCalls.isEmpty())
        assertTrue(emailRepository.replyCalls.isEmpty())
    }

    @Test
    fun `a second sendDraft is ignored while one is already in flight`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(draftDetail("d1"))))
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository, emailRepository)
        val gate = CompletableDeferred<Unit>()
        emailRepository.sendGate = gate

        viewModel.sendDraft("d1")
        assertTrue(viewModel.state.value.actionInProgress)

        viewModel.sendDraft("d1") // should be ignored by the actionInProgress guard

        assertEquals(1, emailRepository.sendCalls.size, "second sendDraft should not have dispatched a request")

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.actionInProgress)
    }
}
