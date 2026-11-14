package com.sonicstarsolutions.agentic.inbox.ui.compose

import com.sonicstarsolutions.agentic.inbox.domain.model.Draft
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ComposeDraftTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun original() = EmailDetail(
        id = "e1",
        subject = "Hello",
        sender = "Alice <alice@example.dev>",
        recipient = "me@example.dev",
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
        mode: ComposeMode = ComposeMode.NEW,
        emailId: String? = null,
        threadId: String? = null,
        draftId: String? = null,
        draftRepository: FakeDraftRepository = FakeDraftRepository(),
        emailRepository: FakeEmailRepository = FakeEmailRepository(),
        newDraftId: () -> String = { "draft-1" },
    ): ComposeViewModel {
        val mailboxRepository = FakeMailboxRepository().apply {
            getMailboxResult = { Result.success(Mailbox(id = "mb1", email = "me@example.dev", name = "Me")) }
        }
        return ComposeViewModel(
            getMailbox = GetMailboxUseCase(mailboxRepository),
            getThread = GetThreadUseCase(FakeThreadRepository(result = Result.success(listOf(original())))),
            sendEmailUseCase = SendEmailUseCase(emailRepository),
            replyEmailUseCase = ReplyEmailUseCase(emailRepository),
            forwardEmailUseCase = ForwardEmailUseCase(emailRepository),
            deleteEmailUseCase = DeleteEmailUseCase(emailRepository),
            saveDraftUseCase = SaveDraftUseCase(draftRepository),
            getDraftUseCase = GetDraftUseCase(draftRepository),
            deleteDraftUseCase = DeleteDraftUseCase(draftRepository),
            // Stands in for the app-lifetime scope. Unconfined on the test's own scheduler, so
            // draft writes land eagerly and share the test's virtual clock.
            externalScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            mailboxId = "mb1",
            mode = mode,
            emailId = emailId,
            threadId = threadId,
            draftId = draftId,
            newDraftId = newDraftId,
            now = { 1_000L },
        )
    }

    @Test
    fun `typing autosaves a draft once the debounce elapses`() = runTest {
        val drafts = FakeDraftRepository()
        val viewModel = buildViewModel(draftRepository = drafts)

        viewModel.onToChanged("bob@example.dev")
        viewModel.onSubjectChanged("Lunch")
        assertTrue(drafts.saveCalls.isEmpty(), "should not save on every keystroke")

        advanceTimeBy(ComposeViewModel.AUTOSAVE_DEBOUNCE_MILLIS + 1)

        val draft = drafts.saved.single()
        assertEquals("draft-1", draft.id)
        assertEquals("mb1", draft.mailboxId)
        assertEquals("bob@example.dev", draft.to)
        assertEquals("Lunch", draft.subject)
        assertEquals(ComposeMode.NEW.name, draft.mode)
        assertEquals("draft-1", viewModel.state.value.draftId)
    }

    @Test
    fun `repeated edits collapse into a single save`() = runTest {
        val drafts = FakeDraftRepository()
        val viewModel = buildViewModel(draftRepository = drafts)

        viewModel.onBodyChanged("a")
        advanceTimeBy(ComposeViewModel.AUTOSAVE_DEBOUNCE_MILLIS / 2)
        viewModel.onBodyChanged("ab")
        advanceTimeBy(ComposeViewModel.AUTOSAVE_DEBOUNCE_MILLIS / 2)
        viewModel.onBodyChanged("abc")
        advanceTimeBy(ComposeViewModel.AUTOSAVE_DEBOUNCE_MILLIS + 1)

        assertEquals(1, drafts.saveCalls.size)
        assertEquals("abc", drafts.saved.single().body)
    }

    @Test
    fun `an untouched composer never creates a draft`() = runTest {
        val drafts = FakeDraftRepository()
        val viewModel = buildViewModel(draftRepository = drafts)

        viewModel.saveDraftNow()
        advanceUntilIdle()

        assertTrue(drafts.saveCalls.isEmpty())
    }

    @Test
    fun `an untouched reply never creates a draft despite its prefilled body`() = runTest {
        // Reply/forward arrive with quoted text already in the body, so an emptiness check alone
        // would save one; only an actual edit should.
        val drafts = FakeDraftRepository()
        val viewModel = buildViewModel(
            mode = ComposeMode.REPLY,
            emailId = "e1",
            threadId = "t1",
            draftRepository = drafts,
        )
        advanceUntilIdle()
        assertTrue(viewModel.state.value.body.isNotBlank(), "precondition: the reply is prefilled")

        viewModel.saveDraftNow()
        advanceUntilIdle()

        assertTrue(drafts.saveCalls.isEmpty(), "opening a reply and backing out must leave no draft")
    }

    @Test
    fun `opening a saved draft and closing it without edits does not rewrite it`() = runTest {
        // A resumed draft already has an id, so nothing else stops a write here — only the fact
        // that the user changed nothing. Rewriting would bump updatedAt and silently reshuffle
        // the Drafts folder, which is sorted by it.
        val existing = Draft(id = "d9", mailboxId = "mb1", body = "text", mode = ComposeMode.NEW.name, updatedAt = 5L)
        val drafts = FakeDraftRepository(initial = listOf(existing))
        val viewModel = buildViewModel(draftId = "d9", draftRepository = drafts)
        advanceUntilIdle()

        viewModel.saveDraftNow()
        advanceUntilIdle()

        assertTrue(drafts.saveCalls.isEmpty(), "an unedited draft must not be rewritten")
        assertEquals(5L, drafts.saved.single().updatedAt, "updatedAt must be untouched")
    }

    @Test
    fun `saveDraftNow saves immediately without waiting for the debounce`() = runTest {
        val drafts = FakeDraftRepository()
        val viewModel = buildViewModel(draftRepository = drafts)

        viewModel.onBodyChanged("half-written")
        viewModel.saveDraftNow()
        advanceUntilIdle()

        assertEquals("half-written", drafts.saved.single().body)
    }

    @Test
    fun `a save starting while another is in flight cannot claim a second draft id`() = runTest {
        // Each mint returns a distinct id, so claiming twice is visible. A constant id would
        // collapse both into one upsert and hide the bug entirely.
        var minted = 0
        val drafts = FakeDraftRepository()
        val viewModel = buildViewModel(draftRepository = drafts, newDraftId = { "draft-${minted++}" })

        // Hold the first autosave inside the repository write, the way a real DAO write takes
        // time, then start a second save while it's still in there. If the id is only claimed
        // once the write completes, both saves see "no draft yet" and mint one each — two rows
        // in the Drafts folder for a single message.
        val gate = CompletableDeferred<Unit>()
        drafts.saveGate = gate
        viewModel.onBodyChanged("text")
        advanceTimeBy(ComposeViewModel.AUTOSAVE_DEBOUNCE_MILLIS + 1)

        viewModel.saveDraftNow()
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, minted, "the draft id must be claimed once per message, not once per save")
    }

    @Test
    fun `resuming a draft loads its fields into the composer`() = runTest {
        val existing = Draft(
            id = "d9",
            mailboxId = "mb1",
            to = "carol@example.dev",
            cc = "dave@example.dev",
            subject = "Re: Hello",
            body = "Half a reply",
            mode = ComposeMode.REPLY.name,
            originalEmailId = "e1",
            threadId = "t1",
            updatedAt = 5L,
        )
        val drafts = FakeDraftRepository(initial = listOf(existing))

        val viewModel = buildViewModel(
            mode = ComposeMode.REPLY,
            emailId = "e1",
            threadId = "t1",
            draftId = "d9",
            draftRepository = drafts,
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("carol@example.dev", state.to)
        assertEquals("dave@example.dev", state.cc)
        assertEquals("Re: Hello", state.subject)
        assertEquals("Half a reply", state.body)
        assertEquals("d9", state.draftId)
    }

    @Test
    fun `a resumed draft keeps its id instead of forking a new one`() = runTest {
        val existing = Draft(id = "d9", mailboxId = "mb1", body = "text", mode = ComposeMode.NEW.name, updatedAt = 5L)
        val drafts = FakeDraftRepository(initial = listOf(existing))
        val viewModel = buildViewModel(draftId = "d9", draftRepository = drafts)
        advanceUntilIdle()

        viewModel.onBodyChanged("text edited")
        advanceTimeBy(ComposeViewModel.AUTOSAVE_DEBOUNCE_MILLIS + 1)

        assertEquals(listOf("d9"), drafts.saved.map { it.id })
        assertEquals("text edited", drafts.saved.single().body)
    }

    @Test
    fun `discardDraft deletes it and signals the screen to leave`() = runTest {
        val existing = Draft(id = "d9", mailboxId = "mb1", body = "text", mode = ComposeMode.NEW.name, updatedAt = 5L)
        val drafts = FakeDraftRepository(initial = listOf(existing))
        val viewModel = buildViewModel(draftId = "d9", draftRepository = drafts)
        advanceUntilIdle()

        viewModel.discardDraft()
        advanceUntilIdle()

        assertEquals(listOf("d9"), drafts.deleteCalls)
        assertTrue(drafts.saved.isEmpty())
        assertTrue(viewModel.state.value.discarded)
    }

    @Test
    fun `a pending autosave never resurrects a discarded draft`() = runTest {
        val drafts = FakeDraftRepository()
        val viewModel = buildViewModel(draftRepository = drafts)

        viewModel.onBodyChanged("text")
        viewModel.discardDraft()
        advanceTimeBy(ComposeViewModel.AUTOSAVE_DEBOUNCE_MILLIS + 1)

        assertTrue(drafts.saved.isEmpty(), "the scheduled autosave should have been cancelled")
    }

    @Test
    fun `sending deletes the draft it was composed from`() = runTest {
        val existing = Draft(
            id = "d9",
            mailboxId = "mb1",
            to = "bob@example.dev",
            body = "text",
            mode = ComposeMode.NEW.name,
            updatedAt = 5L,
        )
        val drafts = FakeDraftRepository(initial = listOf(existing))
        val viewModel = buildViewModel(draftId = "d9", draftRepository = drafts)
        advanceUntilIdle()

        viewModel.send()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.sent)
        assertEquals(listOf("d9"), drafts.deleteCalls)
        assertTrue(drafts.saved.isEmpty())
    }

    @Test
    fun `a failed send keeps the draft`() = runTest {
        val existing = Draft(
            id = "d9",
            mailboxId = "mb1",
            to = "bob@example.dev",
            body = "text",
            mode = ComposeMode.NEW.name,
            updatedAt = 5L,
        )
        val drafts = FakeDraftRepository(initial = listOf(existing))
        val viewModel = buildViewModel(
            draftId = "d9",
            draftRepository = drafts,
            emailRepository = FakeEmailRepository(sendResult = Result.failure(RuntimeException("offline"))),
        )
        advanceUntilIdle()

        viewModel.send()
        advanceUntilIdle()

        assertTrue(drafts.deleteCalls.isEmpty(), "a draft must survive a send that failed")
        assertEquals("offline", viewModel.state.value.errorMessage)
    }
}
