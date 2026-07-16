package com.sonicstarsolutions.agentic.inbox.ui.thread

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetThreadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.repository.ThreadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    private fun detail(id: String) = EmailDetail(
        id = id,
        subject = "Subject",
        sender = "a@example.dev",
        recipient = "b@example.dev",
        cc = null,
        bcc = null,
        date = "2026-07-16T00:00:00Z",
        read = true,
        starred = false,
        threadId = "t1",
        folderId = "inbox",
        body = "<p>Body $id</p>",
        attachments = emptyList(),
    )

    private fun buildViewModel(repository: FakeThreadRepository): ThreadViewModel = ThreadViewModel(
        getThread = GetThreadUseCase(repository),
        mailboxId = "mb1",
        emailId = "e1",
        threadId = "t1",
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
}
