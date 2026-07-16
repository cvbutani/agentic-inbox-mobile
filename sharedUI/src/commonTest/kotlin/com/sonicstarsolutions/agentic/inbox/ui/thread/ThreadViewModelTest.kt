package com.sonicstarsolutions.agentic.inbox.ui.thread

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
import com.sonicstarsolutions.agentic.inbox.domain.repository.ThreadRepository
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetFoldersUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetThreadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.MarkThreadReadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.MoveEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SetEmailReadUseCase
import com.sonicstarsolutions.agentic.inbox.testutil.FakeEmailRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeFolderRepository
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

private class FakeThreadRepository(
    var result: Result<List<EmailDetail>> = Result.success(emptyList()),
) : ThreadRepository {
    override suspend fun getThread(mailboxId: String, emailId: String, threadId: String?): Result<List<EmailDetail>> = result
}

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

    private fun detail(id: String, read: Boolean = true) = EmailDetail(
        id = id,
        subject = "Subject",
        sender = "a@example.dev",
        recipient = "b@example.dev",
        cc = null,
        bcc = null,
        date = "2026-07-16T00:00:00Z",
        read = read,
        starred = false,
        threadId = "t1",
        folderId = "inbox",
        body = "<p>Body $id</p>",
        attachments = emptyList(),
    )

    private fun buildViewModel(
        threadRepository: FakeThreadRepository,
        emailRepository: FakeEmailRepository = FakeEmailRepository(),
        folderRepository: FakeFolderRepository = FakeFolderRepository(),
        threadId: String? = "t1",
    ): ThreadViewModel = ThreadViewModel(
        getThread = GetThreadUseCase(threadRepository),
        getFolders = GetFoldersUseCase(folderRepository),
        moveEmail = MoveEmailUseCase(emailRepository),
        deleteEmail = DeleteEmailUseCase(emailRepository),
        setEmailRead = SetEmailReadUseCase(emailRepository),
        markThreadRead = MarkThreadReadUseCase(emailRepository),
        mailboxId = "mb1",
        emailId = "e1",
        threadId = threadId,
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
    fun `failure surfaces an error message`() = runTest {
        val repository = FakeThreadRepository(result = Result.failure(RuntimeException("not found")))

        val viewModel = buildViewModel(repository)

        assertFalse(viewModel.state.value.loading)
        assertEquals("not found", viewModel.state.value.errorMessage)
        assertTrue(viewModel.state.value.messages.isEmpty())
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

        viewModel.toggleReadState()

        assertEquals(listOf(FakeEmailRepository.SetReadCall("mb1", "e1", false)), emailRepository.setReadCalls)
        assertFalse(viewModel.state.value.messages.single().read)
        val result = viewModel.state.value.actionResult
        assertTrue(result is ThreadActionResult.Success)
        assertFalse(result.shouldNavigateBack)
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
}
