package com.sonicstarsolutions.agentic.inbox.ui.compose

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * EDIT_DRAFT resumes a draft that's a real row on the server (sitting in the `draft` folder —
 * see cloudflare/agentic-inbox's workers/index.ts /drafts handler), not this app's own local-only
 * Draft (Room) feature covered by ComposeDraftTest. It's reached from a message already sitting in
 * ThreadUiState.messages, so prefill comes straight from that message's own fields rather than
 * being derived the way a fresh Reply/Forward would (ComposePrefill never runs for this mode).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ComposeEditDraftTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun draftMessage(
        id: String = "d1",
        inReplyTo: String? = null,
        recipient: String = "carol@example.dev",
        cc: String? = "dave@example.dev",
        subject: String = "Re: Hello", // deliberately already has the prefix — proves no re-derivation happens
        body: String = "half-written reply",
    ) = EmailDetail(
        id = id,
        subject = subject,
        sender = "me@example.dev",
        recipient = recipient,
        cc = cc,
        bcc = null,
        date = "2026-07-17T00:00:00Z",
        read = true,
        starred = false,
        threadId = "t1",
        folderId = SystemFolders.DRAFT,
        body = body,
        attachments = emptyList(),
        inReplyTo = inReplyTo,
    )

    private fun TestScope.buildViewModel(
        draftMessageId: String = "d1",
        threadRepository: FakeThreadRepository,
        emailRepository: FakeEmailRepository = FakeEmailRepository(),
        mailboxRepository: FakeMailboxRepository = FakeMailboxRepository().apply {
            getMailboxResult = { Result.success(Mailbox(id = "mb1", email = "me@example.dev", name = "Me")) }
        },
    ): ComposeViewModel = ComposeViewModel(
        getMailbox = GetMailboxUseCase(mailboxRepository),
        getThread = GetThreadUseCase(threadRepository),
        sendEmailUseCase = SendEmailUseCase(emailRepository),
        replyEmailUseCase = ReplyEmailUseCase(emailRepository),
        forwardEmailUseCase = ForwardEmailUseCase(emailRepository),
        deleteEmailUseCase = DeleteEmailUseCase(emailRepository),
        saveDraftUseCase = SaveDraftUseCase(FakeDraftRepository()),
        getDraftUseCase = GetDraftUseCase(FakeDraftRepository()),
        deleteDraftUseCase = DeleteDraftUseCase(FakeDraftRepository()),
        externalScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        mailboxId = "mb1",
        mode = ComposeMode.EDIT_DRAFT,
        emailId = draftMessageId,
        threadId = "t1",
    )

    @Test
    fun `an html draft body is converted to editable text for the plain-text composer`() = runTest {
        // The Worker's AI writes drafts as HTML. The composer's Message field is a plain
        // TextField — handing it raw markup means the user edits angle brackets.
        val threadRepository = FakeThreadRepository(
            result = Result.success(
                listOf(
                    draftMessage(
                        body = """<div style="white-space:pre-wrap">Sounds good.</div><br>""" +
                            """<blockquote>On Fri, Alice wrote:<br>See you then.</blockquote>""",
                    ),
                ),
            ),
        )

        val viewModel = buildViewModel(threadRepository = threadRepository)

        val body = viewModel.state.value.body
        assertEquals("Sounds good.\n\nOn Fri, Alice wrote:\nSee you then.", body)
    }

    @Test
    fun `a plain text draft body is loaded verbatim`() = runTest {
        val threadRepository = FakeThreadRepository(
            result = Result.success(listOf(draftMessage(body = "Dear Bob,\n\n5 < 6 and a & b."))),
        )

        val viewModel = buildViewModel(threadRepository = threadRepository)

        assertEquals("Dear Bob,\n\n5 < 6 and a & b.", viewModel.state.value.body)
    }

    @Test
    fun `prefills directly from the draft message's own fields rather than deriving reply text`() = runTest {
        val threadRepository = FakeThreadRepository(result = Result.success(listOf(draftMessage())))

        val viewModel = buildViewModel(threadRepository = threadRepository)

        val state = viewModel.state.value
        assertEquals("carol@example.dev", state.to)
        assertEquals("dave@example.dev", state.cc)
        assertEquals("Re: Hello", state.subject) // unchanged — not re-prefixed
        assertEquals("half-written reply", state.body) // unchanged — not quoted
        assertFalse(state.loading)
    }

    @Test
    fun `send with inReplyTo targets the original message via replyEmail, not the draft's own id`() = runTest {
        val threadRepository = FakeThreadRepository(
            result = Result.success(listOf(draftMessage(id = "d1", inReplyTo = "original1"))),
        )
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository = threadRepository, emailRepository = emailRepository)

        viewModel.send()

        val call = emailRepository.replyCalls.single()
        assertEquals("original1", call.emailId)
        assertTrue(emailRepository.sendCalls.isEmpty())
        assertTrue(viewModel.state.value.sent)
    }

    @Test
    fun `send without inReplyTo dispatches via plain sendEmail`() = runTest {
        val threadRepository = FakeThreadRepository(
            result = Result.success(listOf(draftMessage(id = "d1", inReplyTo = null))),
        )
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository = threadRepository, emailRepository = emailRepository)

        viewModel.send()

        assertEquals(1, emailRepository.sendCalls.size)
        assertTrue(emailRepository.replyCalls.isEmpty())
    }

    @Test
    fun `edited fields are what actually gets sent, not the original draft's fields`() = runTest {
        val threadRepository = FakeThreadRepository(
            result = Result.success(listOf(draftMessage(id = "d1", inReplyTo = "original1"))),
        )
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(threadRepository = threadRepository, emailRepository = emailRepository)

        viewModel.onBodyChanged("actually, let me rewrite this")
        viewModel.send()

        assertEquals("actually, let me rewrite this", emailRepository.replyCalls.single().request.body)
    }

    @Test
    fun `a successful send deletes the original draft row from the server`() = runTest {
        val threadRepository = FakeThreadRepository(
            result = Result.success(listOf(draftMessage(id = "d1", inReplyTo = "original1"))),
        )
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(draftMessageId = "d1", threadRepository = threadRepository, emailRepository = emailRepository)

        viewModel.send()
        advanceUntilIdle()

        assertEquals(listOf(FakeEmailRepository.DeleteCall("mb1", "d1")), emailRepository.deleteCalls)
    }

    @Test
    fun `a failed send does not delete the draft`() = runTest {
        val threadRepository = FakeThreadRepository(
            result = Result.success(listOf(draftMessage(id = "d1", inReplyTo = "original1"))),
        )
        val emailRepository = FakeEmailRepository(replyResult = Result.failure(RuntimeException("offline")))
        val viewModel = buildViewModel(draftMessageId = "d1", threadRepository = threadRepository, emailRepository = emailRepository)

        viewModel.send()

        assertTrue(emailRepository.deleteCalls.isEmpty())
        assertEquals("offline", viewModel.state.value.errorMessage)
    }
}
