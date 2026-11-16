package com.sonicstarsolutions.agentic.inbox.ui.compose

import com.sonicstarsolutions.agentic.inbox.domain.model.Contact
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduleKind
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ForwardEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.repository.ContactRepository
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetThreadUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ReplyEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SaveDraftUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ScheduleEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SearchContactsUseCase
import com.sonicstarsolutions.agentic.inbox.testutil.FakeContactRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeLastSenderStore
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxesUseCase
import com.sonicstarsolutions.agentic.inbox.testutil.FakeScheduleRepository
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
        scheduleRepository: FakeScheduleRepository = FakeScheduleRepository(),
        contactRepository: ContactRepository = FakeContactRepository(),
        draftRepository: FakeDraftRepository = FakeDraftRepository(),
        domain: String? = null,
        lastSender: FakeLastSenderStore = FakeLastSenderStore(),
    ): ComposeViewModel = ComposeViewModel(
        getMailbox = GetMailboxUseCase(mailboxRepository),
        getThread = GetThreadUseCase(threadRepository),
        sendEmailUseCase = SendEmailUseCase(emailRepository),
        replyEmailUseCase = ReplyEmailUseCase(emailRepository),
        forwardEmailUseCase = ForwardEmailUseCase(emailRepository),
        scheduleEmailUseCase = ScheduleEmailUseCase(scheduleRepository),
        searchContacts = SearchContactsUseCase(contactRepository),
        deleteEmailUseCase = DeleteEmailUseCase(emailRepository),
        saveDraftUseCase = SaveDraftUseCase(draftRepository),
        getDraftUseCase = GetDraftUseCase(draftRepository),
        deleteDraftUseCase = DeleteDraftUseCase(draftRepository),
        getMailboxes = GetMailboxesUseCase(mailboxRepository),
        lastSenderStore = lastSender,
        externalScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        mailboxId = if (domain == null) "mb1" else "",
        mode = mode,
        emailId = emailId,
        threadId = threadId,
        domain = domain,
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

    // --- aliases and the From picker -------------------------------------------------------

    private fun aliasMailboxRepository() = FakeMailboxRepository().apply {
        getMailboxResult = {
            Result.success(Mailbox(id = "mb1", email = "me@example.dev", name = "Me", aliases = listOf("sales@example.dev")))
        }
    }

    @Test
    fun `a mailbox with aliases offers them as From choices, the mailbox first`() = runTest {
        val viewModel = buildViewModel(ComposeMode.NEW, mailboxRepository = aliasMailboxRepository())

        assertEquals(listOf("me@example.dev", "sales@example.dev"), viewModel.state.value.fromOptions)
        assertEquals("me@example.dev", viewModel.state.value.selectedFrom)
    }

    @Test
    fun `a mailbox without aliases offers no choice`() = runTest {
        val viewModel = buildViewModel(ComposeMode.NEW)

        assertTrue(viewModel.state.value.fromOptions.size <= 1)
        assertEquals("me@example.dev", viewModel.state.value.selectedFrom)
    }

    @Test
    fun `choosing an alias sends from it`() = runTest {
        val emailRepository = FakeEmailRepository()
        val viewModel = buildViewModel(ComposeMode.NEW, mailboxRepository = aliasMailboxRepository(), emailRepository = emailRepository)
        viewModel.onToChanged("alice@example.dev")
        viewModel.onSubjectChanged("Hi")
        viewModel.onBodyChanged("Hello")

        viewModel.onFromSelected("sales@example.dev")
        viewModel.send()

        assertEquals("sales@example.dev", emailRepository.sendCalls.single().request.fromEmail)
        assertEquals("sales@example.dev", viewModel.state.value.selectedFrom)
    }

    @Test
    fun `an address that is not one of the choices is ignored`() = runTest {
        val viewModel = buildViewModel(ComposeMode.NEW, mailboxRepository = aliasMailboxRepository())

        viewModel.onFromSelected("stranger@example.org")

        assertEquals("me@example.dev", viewModel.state.value.selectedFrom)
    }

    @Test
    fun `replying to mail sent to an alias defaults to that alias`() = runTest {
        val viewModel = buildViewModel(
            ComposeMode.REPLY,
            emailId = "e1",
            threadId = "t1",
            mailboxRepository = aliasMailboxRepository(),
            threadRepository = FakeThreadRepository(
                result = Result.success(listOf(original(sender = "Alice <alice@example.dev>", recipient = "sales@example.dev"))),
            ),
        )

        assertEquals("sales@example.dev", viewModel.state.value.selectedFrom)
    }

    @Test
    fun `composing a new message defaults to the mailbox even when it has aliases`() = runTest {
        val viewModel = buildViewModel(ComposeMode.NEW, mailboxRepository = aliasMailboxRepository())

        assertEquals("me@example.dev", viewModel.state.value.selectedFrom)
    }

    // --- send later ------------------------------------------------------------------------

    private val now = kotlin.time.Instant.parse("2026-11-18T09:00:00Z")
    private val tomorrowMorning = kotlin.time.Instant.parse("2026-11-19T08:00:00Z")

    private fun TestScope.readyToSend(
        mode: ComposeMode = ComposeMode.NEW,
        emailId: String? = null,
        scheduleRepository: FakeScheduleRepository = FakeScheduleRepository(),
        draftRepository: FakeDraftRepository = FakeDraftRepository(),
    ): ComposeViewModel {
        val viewModel = buildViewModel(
            mode,
            emailId = emailId,
            threadId = emailId?.let { "t1" },
            scheduleRepository = scheduleRepository,
            draftRepository = draftRepository,
        )
        if (mode == ComposeMode.NEW) viewModel.onToChanged("alice@example.dev")
        viewModel.onSubjectChanged("Plans")
        viewModel.onBodyChanged("See you then")
        return viewModel
    }

    @Test
    fun `scheduling a new message queues a compose for the chosen time`() = runTest {
        val schedule = FakeScheduleRepository()
        val viewModel = readyToSend(scheduleRepository = schedule)

        viewModel.scheduleSend(tomorrowMorning, now)

        val call = schedule.scheduleCalls.single()
        assertEquals(ScheduleKind.COMPOSE, call.kind)
        assertNull(call.originalEmailId)
        assertEquals(tomorrowMorning, call.sendAt)
        assertEquals(listOf("alice@example.dev"), call.request.to)
        assertEquals("Plans", call.request.subject)
        assertEquals("me@example.dev", call.request.fromEmail)
        assertTrue(viewModel.state.value.sent)
    }

    @Test
    fun `scheduling a reply names the message being answered`() = runTest {
        val schedule = FakeScheduleRepository()
        val viewModel = readyToSend(ComposeMode.REPLY, emailId = "e1", scheduleRepository = schedule)

        viewModel.scheduleSend(tomorrowMorning, now)

        val call = schedule.scheduleCalls.single()
        assertEquals(ScheduleKind.REPLY, call.kind)
        assertEquals("e1", call.originalEmailId)
    }

    @Test
    fun `scheduling a forward is a plain compose`() = runTest {
        val schedule = FakeScheduleRepository()
        val viewModel = readyToSend(ComposeMode.FORWARD, emailId = "e1", scheduleRepository = schedule)
        viewModel.onToChanged("carol@example.dev")

        viewModel.scheduleSend(tomorrowMorning, now)

        assertEquals(ScheduleKind.COMPOSE, schedule.scheduleCalls.single().kind)
        assertNull(schedule.scheduleCalls.single().originalEmailId)
    }

    @Test
    fun `scheduling needs a recipient`() = runTest {
        val schedule = FakeScheduleRepository()
        val viewModel = buildViewModel(ComposeMode.NEW, scheduleRepository = schedule)

        viewModel.scheduleSend(tomorrowMorning, now)

        assertTrue(schedule.scheduleCalls.isEmpty())
        assertEquals("At least one recipient is required.", viewModel.state.value.errorMessage)
    }

    @Test
    fun `a message with attachments cannot be scheduled`() = runTest {
        val schedule = FakeScheduleRepository()
        val viewModel = readyToSend(scheduleRepository = schedule)
        viewModel.addAttachment("a.txt", "text/plain", "hi".encodeToByteArray())

        viewModel.scheduleSend(tomorrowMorning, now)

        assertTrue(schedule.scheduleCalls.isEmpty())
        assertEquals("Scheduled messages can't have attachments yet. Remove them, or send now.", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.sent)
    }

    @Test
    fun `a time that is too soon or too far is refused before any request`() = runTest {
        val schedule = FakeScheduleRepository()
        val viewModel = readyToSend(scheduleRepository = schedule)

        viewModel.scheduleSend(now, now)
        assertEquals("Pick a time at least a couple of minutes from now.", viewModel.state.value.errorMessage)

        viewModel.scheduleSend(kotlin.time.Instant.parse("2027-06-01T09:00:00Z"), now)
        assertEquals("Pick a time within the next 90 days.", viewModel.state.value.errorMessage)

        assertTrue(schedule.scheduleCalls.isEmpty())
    }

    @Test
    fun `a refused schedule keeps the composer open with the server's reason`() = runTest {
        val schedule = FakeScheduleRepository(scheduleResult = Result.failure(RuntimeException("Scheduled messages cannot have attachments yet")))
        val viewModel = readyToSend(scheduleRepository = schedule)

        viewModel.scheduleSend(tomorrowMorning, now)

        assertEquals("Scheduled messages cannot have attachments yet", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.sent)
        assertFalse(viewModel.state.value.sending)
    }

    // --- recipient autocomplete ------------------------------------------------------------

    private val bob = Contact("bob@example.dev", 7)
    private val boris = Contact("boris@example.dev", 2)

    private fun contacts(vararg all: Contact) = FakeContactRepository { query ->
        Result.success(all.filter { it.address.contains(query.lowercase()) })
    }

    @Test
    fun `typing in To suggests contacts for the word being typed after a short pause`() = runTest {
        val repo = contacts(bob, boris)
        val viewModel = buildViewModel(ComposeMode.NEW, contactRepository = repo)

        viewModel.onToChanged("alice@example.dev, bo")
        advanceTimeBy(ComposeViewModel.SUGGEST_DEBOUNCE_MILLIS + 1)

        assertEquals(listOf("bo"), repo.queries)
        assertEquals(listOf(bob, boris), viewModel.state.value.suggestions)
        assertEquals(ComposeField.TO, viewModel.state.value.suggestionsFor)
    }

    @Test
    fun `nothing is requested before the pause ends, and fast typing makes one request`() = runTest {
        val repo = contacts(bob)
        val viewModel = buildViewModel(ComposeMode.NEW, contactRepository = repo)

        viewModel.onToChanged("b")
        advanceTimeBy(ComposeViewModel.SUGGEST_DEBOUNCE_MILLIS / 2)
        viewModel.onToChanged("bo")
        advanceTimeBy(ComposeViewModel.SUGGEST_DEBOUNCE_MILLIS / 2)
        assertTrue(repo.queries.isEmpty())

        advanceTimeBy(ComposeViewModel.SUGGEST_DEBOUNCE_MILLIS)
        assertEquals(listOf("bo"), repo.queries)
    }

    @Test
    fun `a blank word clears suggestions and asks nothing`() = runTest {
        val repo = contacts(bob)
        val viewModel = buildViewModel(ComposeMode.NEW, contactRepository = repo)
        viewModel.onToChanged("bo")
        advanceTimeBy(ComposeViewModel.SUGGEST_DEBOUNCE_MILLIS + 1)
        assertEquals(1, viewModel.state.value.suggestions.size)

        viewModel.onToChanged("bob@example.dev, ")
        advanceTimeBy(ComposeViewModel.SUGGEST_DEBOUNCE_MILLIS + 1)

        assertTrue(viewModel.state.value.suggestions.isEmpty())
        assertEquals(listOf("bo"), repo.queries)
    }

    @Test
    fun `addresses already in the field and the user's own addresses are not suggested`() = runTest {
        val repo = contacts(bob, boris, Contact("me@example.dev", 50), Contact("sales@example.dev", 9))
        val viewModel = buildViewModel(ComposeMode.NEW, mailboxRepository = aliasMailboxRepository(), contactRepository = repo)

        viewModel.onToChanged("bob@example.dev, ")
        viewModel.onToChanged("bob@example.dev, e")
        advanceTimeBy(ComposeViewModel.SUGGEST_DEBOUNCE_MILLIS + 1)

        assertEquals(listOf(boris), viewModel.state.value.suggestions)
    }

    @Test
    fun `picking a suggestion completes the word, adds a separator and closes the list`() = runTest {
        val viewModel = buildViewModel(ComposeMode.NEW, contactRepository = contacts(bob))
        viewModel.onToChanged("alice@example.dev, bo")
        advanceTimeBy(ComposeViewModel.SUGGEST_DEBOUNCE_MILLIS + 1)

        viewModel.onSuggestionPicked(bob)

        assertEquals("alice@example.dev, bob@example.dev, ", viewModel.state.value.to)
        assertTrue(viewModel.state.value.suggestions.isEmpty())
        assertNull(viewModel.state.value.suggestionsFor)
    }

    @Test
    fun `suggestions follow the Cc and Bcc fields too`() = runTest {
        val viewModel = buildViewModel(ComposeMode.NEW, contactRepository = contacts(bob))
        viewModel.onCcChanged("bo")
        advanceTimeBy(ComposeViewModel.SUGGEST_DEBOUNCE_MILLIS + 1)
        assertEquals(ComposeField.CC, viewModel.state.value.suggestionsFor)

        viewModel.onSuggestionPicked(bob)
        assertEquals("bob@example.dev, ", viewModel.state.value.cc)
        assertEquals("", viewModel.state.value.to)

        viewModel.onBccChanged("bo")
        advanceTimeBy(ComposeViewModel.SUGGEST_DEBOUNCE_MILLIS + 1)
        viewModel.onSuggestionPicked(bob)
        assertEquals("bob@example.dev, ", viewModel.state.value.bcc)
    }

    @Test
    fun `a failed lookup just leaves no suggestions and never shows an error`() = runTest {
        val repo = FakeContactRepository { Result.failure(RuntimeException("offline")) }
        val viewModel = buildViewModel(ComposeMode.NEW, contactRepository = repo)

        viewModel.onToChanged("bo")
        advanceTimeBy(ComposeViewModel.SUGGEST_DEBOUNCE_MILLIS + 1)

        assertTrue(viewModel.state.value.suggestions.isEmpty())
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `dismissing the suggestions closes the list without touching the field`() = runTest {
        val viewModel = buildViewModel(ComposeMode.NEW, contactRepository = contacts(bob))
        viewModel.onToChanged("bo")
        advanceTimeBy(ComposeViewModel.SUGGEST_DEBOUNCE_MILLIS + 1)

        viewModel.dismissSuggestions()

        assertTrue(viewModel.state.value.suggestions.isEmpty())
        assertEquals("bo", viewModel.state.value.to)
    }

    @Test
    fun `an answer that arrives after the user moved on is dropped`() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val repo = object : ContactRepository {
            override suspend fun search(mailboxId: String, query: String, limit: Int): Result<List<Contact>> {
                if (query == "bo") gate.await()
                return Result.success(listOf(bob).filter { it.address.contains(query) })
            }
        }
        val viewModel = buildViewModel(ComposeMode.NEW, contactRepository = repo)

        viewModel.onToChanged("bo")
        advanceTimeBy(ComposeViewModel.SUGGEST_DEBOUNCE_MILLIS + 1) // "bo" request now waiting
        viewModel.onToChanged("zz")
        advanceTimeBy(ComposeViewModel.SUGGEST_DEBOUNCE_MILLIS + 1)
        gate.complete(Unit)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.suggestions.isEmpty(), "stale answer for 'bo' must not show for 'zz'")
    }

    // --- composing from the domain-wide "All inboxes" ------------------------------------------

    private val aliceMb = Mailbox(id = "alice@example.com", email = "alice@example.com", name = "alice@example.com", fromName = "Alice Chen", signatureEnabled = true, signatureText = "Alice")
    private val bobMb = Mailbox(id = "bob@example.com", email = "bob@example.com", name = "bob@example.com", fromName = "Bob", aliases = listOf("sales@example.com"), signatureEnabled = true, signatureText = "Bob")
    private val outsiderMb = Mailbox(id = "carol@other.org", email = "carol@other.org", name = "carol@other.org")

    private fun domainMailboxes(): FakeMailboxRepository = FakeMailboxRepository(result = Result.success(listOf(bobMb, outsiderMb, aliceMb))).apply {
        getMailboxResult = { id -> Result.success(listOf(aliceMb, bobMb, outsiderMb).first { it.id == id }) }
    }

    @Test
    fun `from all inboxes the sender choices are the domain's mailboxes, sorted, and no others`() = runTest {
        val viewModel = buildViewModel(ComposeMode.NEW, mailboxRepository = domainMailboxes(), domain = "example.com")
        advanceUntilIdle()

        assertEquals(listOf("alice@example.com", "bob@example.com"), viewModel.state.value.senderMailboxes.map { it.email })
        assertEquals(listOf("Alice Chen", "Bob"), viewModel.state.value.senderMailboxes.map { it.name })
    }

    @Test
    fun `from all inboxes the default sender is the last mailbox sent from`() = runTest {
        val viewModel = buildViewModel(
            ComposeMode.NEW, mailboxRepository = domainMailboxes(), domain = "example.com",
            lastSender = FakeLastSenderStore(mapOf("example.com" to "bob@example.com")),
        )
        advanceUntilIdle()

        assertEquals("bob@example.com", viewModel.state.value.selectedMailboxId)
        assertEquals("Bob <bob@example.com>", viewModel.state.value.fromAddress)
    }

    @Test
    fun `from all inboxes with nothing remembered the first mailbox is the default`() = runTest {
        val viewModel = buildViewModel(ComposeMode.NEW, mailboxRepository = domainMailboxes(), domain = "example.com")
        advanceUntilIdle()

        assertEquals("alice@example.com", viewModel.state.value.selectedMailboxId)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun `the default sender's signature and aliases are loaded`() = runTest {
        val viewModel = buildViewModel(
            ComposeMode.NEW, mailboxRepository = domainMailboxes(), domain = "example.com",
            lastSender = FakeLastSenderStore(mapOf("example.com" to "bob@example.com")),
        )
        advanceUntilIdle()

        assertTrue(viewModel.state.value.body.contains("Bob"))
        assertEquals(listOf("bob@example.com", "sales@example.com"), viewModel.state.value.fromOptions)
    }

    @Test
    fun `sending from all inboxes goes to the selected mailbox`() = runTest {
        val emails = FakeEmailRepository()
        val viewModel = buildViewModel(ComposeMode.NEW, mailboxRepository = domainMailboxes(), emailRepository = emails, domain = "example.com")
        advanceUntilIdle()
        viewModel.onSenderMailboxSelected("bob@example.com")
        viewModel.onToChanged("dest@x.dev")
        viewModel.onSubjectChanged("Hi")

        viewModel.send()
        advanceUntilIdle()

        assertEquals("bob@example.com", emails.sendCalls.single().mailboxId)
    }

    @Test
    fun `choosing another mailbox loads its aliases and swaps an untouched signature`() = runTest {
        val viewModel = buildViewModel(ComposeMode.NEW, mailboxRepository = domainMailboxes(), domain = "example.com")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.body.contains("Alice"))

        viewModel.onSenderMailboxSelected("bob@example.com")
        advanceUntilIdle()

        assertEquals("bob@example.com", viewModel.state.value.selectedMailboxId)
        assertEquals("Bob <bob@example.com>", viewModel.state.value.fromAddress)
        assertEquals(listOf("bob@example.com", "sales@example.com"), viewModel.state.value.fromOptions)
        assertTrue(viewModel.state.value.body.contains("Bob"))
        assertFalse(viewModel.state.value.body.contains("Alice"))
    }

    @Test
    fun `choosing another mailbox leaves text the user already wrote alone`() = runTest {
        val viewModel = buildViewModel(ComposeMode.NEW, mailboxRepository = domainMailboxes(), domain = "example.com")
        advanceUntilIdle()
        viewModel.onBodyChanged("My own words")

        viewModel.onSenderMailboxSelected("bob@example.com")
        advanceUntilIdle()

        assertEquals("My own words", viewModel.state.value.body)
        assertEquals("bob@example.com", viewModel.state.value.selectedMailboxId)
    }

    @Test
    fun `an unknown mailbox cannot be chosen`() = runTest {
        val viewModel = buildViewModel(ComposeMode.NEW, mailboxRepository = domainMailboxes(), domain = "example.com")
        advanceUntilIdle()

        viewModel.onSenderMailboxSelected("carol@other.org")
        advanceUntilIdle()

        assertEquals("alice@example.com", viewModel.state.value.selectedMailboxId)
    }

    @Test
    fun `a successful send remembers the mailbox for next time`() = runTest {
        val store = FakeLastSenderStore()
        val viewModel = buildViewModel(ComposeMode.NEW, mailboxRepository = domainMailboxes(), domain = "example.com", lastSender = store)
        advanceUntilIdle()
        viewModel.onSenderMailboxSelected("bob@example.com")
        viewModel.onToChanged("dest@x.dev")
        viewModel.onSubjectChanged("Hi")

        viewModel.send()
        advanceUntilIdle()

        assertEquals("bob@example.com", store.remembered["example.com"])
    }

    @Test
    fun `a failed send does not change the remembered mailbox`() = runTest {
        val store = FakeLastSenderStore(mapOf("example.com" to "alice@example.com"))
        val viewModel = buildViewModel(
            ComposeMode.NEW, mailboxRepository = domainMailboxes(), domain = "example.com", lastSender = store,
            emailRepository = FakeEmailRepository(sendResult = Result.failure(RuntimeException("nope"))),
        )
        advanceUntilIdle()
        viewModel.onSenderMailboxSelected("bob@example.com")
        viewModel.onToChanged("dest@x.dev")
        viewModel.onSubjectChanged("Hi")

        viewModel.send()
        advanceUntilIdle()

        assertEquals("alice@example.com", store.remembered["example.com"])
    }

    @Test
    fun `a single-mailbox compose offers no mailbox choice`() = runTest {
        val viewModel = buildViewModel(ComposeMode.NEW)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.senderMailboxes.isEmpty())
    }
}
