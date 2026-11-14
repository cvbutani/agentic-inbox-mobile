package com.sonicstarsolutions.agentic.inbox.ui.compose

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
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
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ComposeViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun original(
        id: String = "e1",
        subject: String = "Hello",
        sender: String = "Alice <alice@example.dev>",
        recipient: String = "me@example.dev",
    ) = EmailDetail(
        id = id,
        subject = subject,
        sender = sender,
        recipient = recipient,
        cc = null,
        bcc = null,
        date = "2026-07-16T00:00:00Z",
        read = true,
        starred = false,
        threadId = "t1",
        folderId = "inbox",
        body = "<p>Original</p>",
        attachments = emptyList(),
    )

    private fun TestScope.buildViewModel(
        mode: ComposeMode,
        emailId: String? = null,
        threadId: String? = null,
        mailboxRepository: FakeMailboxRepository = FakeMailboxRepository().apply {
            getMailboxResult = { Result.success(Mailbox(id = "mb1", email = "me@example.dev", name = "Me")) }
        },
        threadRepository: FakeThreadRepository = FakeThreadRepository(result = Result.success(listOf(original()))),
        emailRepository: FakeEmailRepository = FakeEmailRepository(),
        draftRepository: FakeDraftRepository = FakeDraftRepository(),
    ): ComposeViewModel = ComposeViewModel(
        getMailbox = GetMailboxUseCase(mailboxRepository),
        getThread = GetThreadUseCase(threadRepository),
        sendEmailUseCase = SendEmailUseCase(emailRepository),
        replyEmailUseCase = ReplyEmailUseCase(emailRepository),
        forwardEmailUseCase = ForwardEmailUseCase(emailRepository),
        deleteEmailUseCase = DeleteEmailUseCase(emailRepository),
        saveDraftUseCase = SaveDraftUseCase(draftRepository),
        getDraftUseCase = GetDraftUseCase(draftRepository),
        deleteDraftUseCase = DeleteDraftUseCase(draftRepository),
        externalScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        mailboxId = "mb1",
        mode = mode,
        emailId = emailId,
        threadId = threadId,
    )

    @Test
    fun `new mode starts with empty fields and not loading`() = runTest {
        val viewModel = buildViewModel(ComposeMode.NEW)

        assertFalse(viewModel.state.value.loading)
        assertEquals("", viewModel.state.value.to)
        assertEquals("", viewModel.state.value.subject)
    }

    @Test
    fun `reply mode prefills the to and subject from the original message`() = runTest {
        val viewModel = buildViewModel(
            ComposeMode.REPLY,
            emailId = "e1",
            threadId = "t1",
            threadRepository = FakeThreadRepository(result = Result.success(listOf(original(subject = "Hello")))),
        )

        assertFalse(viewModel.state.value.loading)
        assertEquals("alice@example.dev", viewModel.state.value.to)
        assertEquals("Re: Hello", viewModel.state.value.subject)
        assertTrue(viewModel.state.value.body.contains("Original"))
    }

    @Test
    fun `reply mode targets the specific message replied from not just the thread's last message`() = runTest {
        // The thread's most recent message is one the mailbox itself sent (to Alice) — replying
        // from the OLDER message (e1, from Alice) should still address Alice, not fall through
        // to whatever the last message in the thread happens to be.
        val viewModel = buildViewModel(
            ComposeMode.REPLY,
            emailId = "e1",
            threadId = "t1",
            threadRepository = FakeThreadRepository(
                result = Result.success(
                    listOf(
                        original(id = "e1", sender = "Alice <alice@example.dev>", recipient = "me@example.dev"),
                        original(id = "e2", sender = "Me <me@example.dev>", recipient = "Alice <alice@example.dev>"),
                    ),
                ),
            ),
        )

        assertEquals("alice@example.dev", viewModel.state.value.to)
    }

    @Test
    fun `reply mode falls back to the last message if the target id is not found in the thread`() = runTest {
        val viewModel = buildViewModel(
            ComposeMode.REPLY,
            emailId = "missing",
            threadId = "t1",
            threadRepository = FakeThreadRepository(
                result = Result.success(listOf(original(id = "e1", sender = "Alice <alice@example.dev>"))),
            ),
        )

        assertEquals("alice@example.dev", viewModel.state.value.to)
    }

    @Test
    fun `forward mode leaves to blank and prefixes the subject with Fwd`() = runTest {
        val viewModel = buildViewModel(
            ComposeMode.FORWARD,
            emailId = "e1",
            threadId = "t1",
            threadRepository = FakeThreadRepository(result = Result.success(listOf(original(subject = "Hello")))),
        )

        assertEquals("", viewModel.state.value.to)
        assertEquals("Fwd: Hello", viewModel.state.value.subject)
    }

    @Test
    fun `send rejects an empty recipient list without calling the repository`() = runTest {
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(ComposeMode.NEW, emailRepository = emailRepository)

        viewModel.send()

        assertTrue(emailRepository.sendCalls.isEmpty())
        assertEquals("At least one recipient is required.", viewModel.state.value.errorMessage)
    }

    @Test
    fun `send in New mode calls sendEmail with the composed request and marks sent`() = runTest {
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(ComposeMode.NEW, emailRepository = emailRepository)

        viewModel.onToChanged("bob@example.dev, carol@example.dev")
        viewModel.onSubjectChanged("  Hi  ")
        viewModel.onBodyChanged("Body text")
        viewModel.send()

        val call = emailRepository.sendCalls.single()
        assertEquals("mb1", call.mailboxId)
        assertEquals("me@example.dev", call.request.fromEmail)
        assertEquals("Me", call.request.fromName)
        assertEquals(listOf("bob@example.dev", "carol@example.dev"), call.request.to)
        assertEquals("Hi", call.request.subject)
        assertEquals("Body text", call.request.body)
        assertTrue(viewModel.state.value.sent)
        assertFalse(viewModel.state.value.sending)
    }

    @Test
    fun `send uses the mailbox's configured fromName over its plain name when set`() = runTest {
        val emailRepository = FakeEmailRepository()
        val mailboxRepository = FakeMailboxRepository().apply {
            getMailboxResult = {
                Result.success(Mailbox(id = "mb1", email = "me@example.dev", name = "Me", fromName = "Marketing Team"))
            }
        }
        val viewModel = buildViewModel(ComposeMode.NEW, emailRepository = emailRepository, mailboxRepository = mailboxRepository)
        viewModel.onToChanged("bob@example.dev")

        viewModel.send()

        assertEquals("Marketing Team", emailRepository.sendCalls.single().request.fromName)
    }

    @Test
    fun `send in Reply mode calls replyEmail for the original email id`() = runTest {
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(ComposeMode.REPLY, emailId = "e1", threadId = "t1", emailRepository = emailRepository)

        viewModel.send()

        val call = emailRepository.replyCalls.single()
        assertEquals("mb1", call.mailboxId)
        assertEquals("e1", call.emailId)
        assertTrue(viewModel.state.value.sent)
    }

    @Test
    fun `send in Forward mode calls forwardEmail for the original email id`() = runTest {
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(ComposeMode.FORWARD, emailId = "e1", threadId = "t1", emailRepository = emailRepository)
        viewModel.onToChanged("someone@example.dev")

        viewModel.send()

        val call = emailRepository.forwardCalls.single()
        assertEquals("mb1", call.mailboxId)
        assertEquals("e1", call.emailId)
        assertTrue(viewModel.state.value.sent)
    }

    @Test
    fun `send surfaces the failure without marking sent`() = runTest {
        val emailRepository = FakeEmailRepository(sendResult = Result.failure(RuntimeException("rejected")))
        val viewModel = buildViewModel(ComposeMode.NEW, emailRepository = emailRepository)
        viewModel.onToChanged("bob@example.dev")

        viewModel.send()

        assertEquals("rejected", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.sent)
        assertFalse(viewModel.state.value.sending)
    }

    @Test
    fun `a second send is ignored while one is already in flight`() = runTest {
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(ComposeMode.NEW, emailRepository = emailRepository)
        viewModel.onToChanged("bob@example.dev")
        val gate = CompletableDeferred<Unit>()
        emailRepository.sendGate = gate

        viewModel.send()
        assertTrue(viewModel.state.value.sending)

        viewModel.send() // should be ignored by the sending guard

        assertEquals(1, emailRepository.sendCalls.size, "second send should not have dispatched a request")

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.sending)
    }

    @Test
    fun `consumeError clears the error message`() = runTest {
        val viewModel = buildViewModel(ComposeMode.NEW)
        viewModel.send() // empty To -> sets an error
        assertTrue(viewModel.state.value.errorMessage != null)

        viewModel.consumeError()

        assertNull(viewModel.state.value.errorMessage)
    }
}
