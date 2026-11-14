package com.sonicstarsolutions.agentic.inbox.ui.compose

import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ForwardEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetThreadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ReplyEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SaveDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SendEmailUseCase
import com.sonicstarsolutions.agentic.inbox.testutil.FakeDraftRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeEmailRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeMailboxRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeThreadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ComposeAttachmentsTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.buildViewModel(
        emailRepository: FakeEmailRepository = FakeEmailRepository(),
    ): ComposeViewModel = ComposeViewModel(
        getMailbox = GetMailboxUseCase(
            FakeMailboxRepository().apply {
                getMailboxResult = { Result.success(Mailbox(id = "mb1", email = "me@example.dev", name = "Me")) }
            },
        ),
        getThread = GetThreadUseCase(FakeThreadRepository()),
        sendEmailUseCase = SendEmailUseCase(emailRepository),
        replyEmailUseCase = ReplyEmailUseCase(emailRepository),
        forwardEmailUseCase = ForwardEmailUseCase(emailRepository),
        deleteEmailUseCase = DeleteEmailUseCase(emailRepository),
        saveDraftUseCase = SaveDraftUseCase(FakeDraftRepository()),
        getDraftUseCase = GetDraftUseCase(FakeDraftRepository()),
        deleteDraftUseCase = DeleteDraftUseCase(FakeDraftRepository()),
        externalScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        mailboxId = "mb1",
        mode = ComposeMode.NEW,
        emailId = null,
        threadId = null,
    )

    @Test
    fun `an added attachment appears in state`() = runTest {
        val viewModel = buildViewModel()

        viewModel.addAttachment("photo.jpg", "image/jpeg", ByteArray(1024))

        val attachment = viewModel.state.value.attachments.single()
        assertEquals("photo.jpg", attachment.filename)
        assertEquals("image/jpeg", attachment.mimeType)
        assertEquals(1024, attachment.bytes.size)
    }

    @Test
    fun `a file over the per-attachment cap is rejected with an error`() = runTest {
        val viewModel = buildViewModel()

        viewModel.addAttachment("huge.zip", "application/zip", ByteArray(ComposeViewModel.MAX_ATTACHMENT_BYTES + 1))

        assertTrue(viewModel.state.value.attachments.isEmpty())
        assertNotNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `attachments beyond the total cap are rejected with an error`() = runTest {
        val viewModel = buildViewModel()
        val chunk = 10 * 1024 * 1024 // 10 MB: two fit under the 25 MB total, a third does not

        viewModel.addAttachment("one.bin", "application/octet-stream", ByteArray(chunk))
        viewModel.addAttachment("two.bin", "application/octet-stream", ByteArray(chunk))
        assertEquals(2, viewModel.state.value.attachments.size)

        viewModel.addAttachment("three.bin", "application/octet-stream", ByteArray(chunk))

        assertEquals(2, viewModel.state.value.attachments.size, "the third chunk crosses the 25 MB total")
        assertNotNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `a removed attachment leaves the rest in order`() = runTest {
        val viewModel = buildViewModel()
        viewModel.addAttachment("a.txt", "text/plain", ByteArray(1))
        viewModel.addAttachment("b.txt", "text/plain", ByteArray(1))
        viewModel.addAttachment("c.txt", "text/plain", ByteArray(1))

        viewModel.removeAttachment(1)

        assertEquals(listOf("a.txt", "c.txt"), viewModel.state.value.attachments.map { it.filename })
    }

    @Test
    fun `send carries the pending attachments in the request`() = runTest {
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(emailRepository)
        viewModel.onToChanged("bob@example.dev")
        viewModel.addAttachment("photo.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

        viewModel.send()

        val request = emailRepository.sendCalls.single().request
        val attachment = request.attachments.single()
        assertEquals("photo.jpg", attachment.filename)
        assertEquals("image/jpeg", attachment.mimeType)
        assertTrue(attachment.bytes.contentEquals(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun `adding an attachment alone does not claim a draft`() = runTest {
        // Local drafts can't persist attachment bytes (v1 limitation) — so an attachment-only
        // change must not create a draft row that would silently resume without its files.
        val viewModel = buildViewModel()

        viewModel.addAttachment("photo.jpg", "image/jpeg", ByteArray(10))

        assertNull(viewModel.state.value.draftId)
    }
}
