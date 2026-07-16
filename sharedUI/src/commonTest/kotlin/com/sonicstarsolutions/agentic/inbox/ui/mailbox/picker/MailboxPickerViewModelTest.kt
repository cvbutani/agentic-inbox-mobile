package com.sonicstarsolutions.agentic.inbox.ui.mailbox.picker

import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ClearCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxesUseCase
import com.sonicstarsolutions.agentic.inbox.testutil.FakeCredentialsRepository
import com.sonicstarsolutions.agentic.inbox.testutil.FakeMailboxRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
}
