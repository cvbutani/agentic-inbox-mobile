package com.sonicstarsolutions.agentic.inbox.ui.mailbox.picker

import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ClearCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CreateMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetAllowedDomainsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxesUseCase
import com.sonicstarsolutions.agentic.inbox.testutil.FakeCredentialsRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeMailboxRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class MailboxPickerViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // refresh() launches on the real Dispatchers.IO (not the Main test dispatcher), so completion
    // isn't guaranteed by the time the launching call returns — wait for the state transition
    // instead of relying on virtual-time advancement.
    private suspend fun MailboxPickerViewModel.awaitLoaded(): MailboxPickerUiState =
        state.first { !it.loading }

    private fun buildViewModel(
        mailboxRepository: FakeMailboxRepository = FakeMailboxRepository(),
        credentialsRepository: FakeCredentialsRepository = FakeCredentialsRepository(),
    ): MailboxPickerViewModel = MailboxPickerViewModel(
        getMailboxes = GetMailboxesUseCase(mailboxRepository),
        clearCredentials = ClearCredentialsUseCase(credentialsRepository),
        createMailboxUseCase = CreateMailboxUseCase(mailboxRepository),
        getAllowedDomains = GetAllowedDomainsUseCase(mailboxRepository),
        deleteMailboxUseCase = DeleteMailboxUseCase(mailboxRepository),
    )

    @Test
    fun `refresh populates mailboxes on success`() = runTest {
        val mailboxes = listOf(Mailbox(id = "mb1", email = "a@example.dev", name = "Alice"))
        val viewModel = buildViewModel(FakeMailboxRepository(result = Result.success(mailboxes)))

        val state = viewModel.awaitLoaded()

        assertEquals(mailboxes, state.mailboxes)
        assertNull(state.errorMessage)
    }

    @Test
    fun `refresh surfaces the failure message`() = runTest {
        val viewModel = buildViewModel(
            FakeMailboxRepository(result = Result.failure(RuntimeException("server down"))),
        )

        val state = viewModel.awaitLoaded()

        assertEquals("server down", state.errorMessage)
        assertTrue(state.mailboxes.isEmpty())
    }

    @Test
    fun `signOut clears credentials and flags signedOut`() = runTest {
        val credentialsRepository = FakeCredentialsRepository()
        val viewModel = buildViewModel(credentialsRepository = credentialsRepository)
        viewModel.awaitLoaded()

        viewModel.signOut()

        assertTrue(viewModel.state.value.signedOut)
    }

    @Test
    fun `consumeSignedOut clears the signedOut flag`() = runTest {
        val viewModel = buildViewModel()
        viewModel.awaitLoaded()
        viewModel.signOut()
        assertTrue(viewModel.state.value.signedOut)

        viewModel.consumeSignedOut()

        assertFalse(viewModel.state.value.signedOut)
    }

    @Test
    fun `allowed domains load into state on init`() = runTest {
        val mailboxRepository = FakeMailboxRepository(allowedDomainsResult = Result.success(listOf("example.dev")))
        val viewModel = buildViewModel(mailboxRepository)
        viewModel.awaitLoaded()

        assertEquals(listOf("example.dev"), viewModel.state.value.allowedDomains)
    }

    @Test
    fun `createMailbox trims fields appends the new mailbox and signals mailboxCreated`() = runTest {
        val mailboxRepository = FakeMailboxRepository(
            createResult = { email, name -> Result.success(Mailbox(id = "mb2", email = email, name = name)) },
        )
        val viewModel = buildViewModel(mailboxRepository)
        viewModel.awaitLoaded()

        viewModel.createMailbox("  sales@example.dev  ", "  Sales  ")

        assertEquals(
            listOf(FakeMailboxRepository.CreateCall("sales@example.dev", "Sales")),
            mailboxRepository.createCalls,
        )
        assertTrue(viewModel.state.value.mailboxes.contains(Mailbox(id = "mb2", email = "sales@example.dev", name = "Sales")))
        assertTrue(viewModel.state.value.mailboxCreated)
        assertFalse(viewModel.state.value.creatingMailbox)
    }

    @Test
    fun `createMailbox rejects a blank email or name without calling the repository`() = runTest {
        val mailboxRepository = FakeMailboxRepository()
        val viewModel = buildViewModel(mailboxRepository)
        viewModel.awaitLoaded()

        viewModel.createMailbox("   ", "Sales")

        assertTrue(mailboxRepository.createCalls.isEmpty())
        assertEquals("Name and email are required.", viewModel.state.value.createMailboxError)
    }

    @Test
    fun `createMailbox surfaces the failure and does not mark mailboxCreated`() = runTest {
        val mailboxRepository = FakeMailboxRepository(createResult = { _, _ -> Result.failure(RuntimeException("address taken")) })
        val viewModel = buildViewModel(mailboxRepository)
        viewModel.awaitLoaded()

        viewModel.createMailbox("sales@example.dev", "Sales")

        assertEquals("address taken", viewModel.state.value.createMailboxError)
        assertFalse(viewModel.state.value.mailboxCreated)
        assertFalse(viewModel.state.value.creatingMailbox)
    }

    @Test
    fun `createMailbox ignores a second call while one is already in flight`() = runTest {
        val mailboxRepository = FakeMailboxRepository()
        val viewModel = buildViewModel(mailboxRepository)
        viewModel.awaitLoaded()
        val gate = CompletableDeferred<Unit>()
        mailboxRepository.createGate = gate

        viewModel.createMailbox("sales@example.dev", "Sales")
        assertTrue(viewModel.state.value.creatingMailbox)

        viewModel.createMailbox("support@example.dev", "Support") // should be ignored

        assertEquals(1, mailboxRepository.createCalls.size, "second createMailbox should not have dispatched a request")

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.creatingMailbox)
    }

    @Test
    fun `consumeMailboxCreated clears the flag`() = runTest {
        val mailboxRepository = FakeMailboxRepository()
        val viewModel = buildViewModel(mailboxRepository)
        viewModel.awaitLoaded()
        viewModel.createMailbox("sales@example.dev", "Sales")
        assertTrue(viewModel.state.value.mailboxCreated)

        viewModel.consumeMailboxCreated()

        assertFalse(viewModel.state.value.mailboxCreated)
    }

    @Test
    fun `deleteMailbox removes the mailbox from state and signals mailboxDeleted`() = runTest {
        val mailboxes = listOf(Mailbox(id = "mb1", email = "a@example.dev", name = "Alice"))
        val mailboxRepository = FakeMailboxRepository(result = Result.success(mailboxes))
        val viewModel = buildViewModel(mailboxRepository)
        viewModel.awaitLoaded()

        viewModel.deleteMailbox("mb1")

        assertEquals(listOf("mb1"), mailboxRepository.deleteCalls)
        assertTrue(viewModel.state.value.mailboxes.isEmpty())
        assertTrue(viewModel.state.value.mailboxDeleted)
        assertFalse(viewModel.state.value.deletingMailbox)
    }

    @Test
    fun `deleteMailbox surfaces the failure and keeps the mailbox in state`() = runTest {
        val mailboxes = listOf(Mailbox(id = "mb1", email = "a@example.dev", name = "Alice"))
        val mailboxRepository = FakeMailboxRepository(result = Result.success(mailboxes))
            .apply { deleteResult = Result.failure(RuntimeException("in use")) }
        val viewModel = buildViewModel(mailboxRepository)
        viewModel.awaitLoaded()

        viewModel.deleteMailbox("mb1")

        assertEquals("in use", viewModel.state.value.deleteMailboxError)
        assertEquals(mailboxes, viewModel.state.value.mailboxes)
        assertFalse(viewModel.state.value.mailboxDeleted)
        assertFalse(viewModel.state.value.deletingMailbox)
    }

    @Test
    fun `deleteMailbox ignores a second call while one is already in flight`() = runTest {
        val mailboxes = listOf(Mailbox(id = "mb1", email = "a@example.dev", name = "Alice"))
        val mailboxRepository = FakeMailboxRepository(result = Result.success(mailboxes))
        val viewModel = buildViewModel(mailboxRepository)
        viewModel.awaitLoaded()
        val gate = CompletableDeferred<Unit>()
        mailboxRepository.deleteGate = gate

        viewModel.deleteMailbox("mb1")
        assertTrue(viewModel.state.value.deletingMailbox)

        viewModel.deleteMailbox("mb1") // should be ignored by the deletingMailbox guard

        assertEquals(1, mailboxRepository.deleteCalls.size, "second deleteMailbox should not have dispatched a request")

        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.deletingMailbox)
    }

    @Test
    fun `consumeMailboxDeleted clears the flag`() = runTest {
        val mailboxes = listOf(Mailbox(id = "mb1", email = "a@example.dev", name = "Alice"))
        val mailboxRepository = FakeMailboxRepository(result = Result.success(mailboxes))
        val viewModel = buildViewModel(mailboxRepository)
        viewModel.awaitLoaded()
        viewModel.deleteMailbox("mb1")
        assertTrue(viewModel.state.value.mailboxDeleted)

        viewModel.consumeMailboxDeleted()

        assertFalse(viewModel.state.value.mailboxDeleted)
    }
}
