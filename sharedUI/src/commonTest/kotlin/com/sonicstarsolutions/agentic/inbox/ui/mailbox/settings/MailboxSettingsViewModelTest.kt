package com.sonicstarsolutions.agentic.inbox.ui.mailbox.settings

import com.sonicstarsolutions.agentic.inbox.data.network.ApiException
import com.sonicstarsolutions.agentic.inbox.domain.model.MailboxSettings
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxSettingsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.UpdateMailboxSettingsUseCase
import com.sonicstarsolutions.agentic.inbox.testutil.FakeMailboxRepository
import kotlinx.coroutines.CompletableDeferred
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

@OptIn(ExperimentalCoroutinesApi::class)
class MailboxSettingsViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val initial = MailboxSettings(
        fromName = "Alice",
        signatureEnabled = true,
        signatureText = "Cheers",
    )

    private fun repository(
        settings: Result<MailboxSettings> = Result.success(initial),
    ) = FakeMailboxRepository().apply { settingsResult = settings }

    private fun buildViewModel(repository: FakeMailboxRepository = repository()) = MailboxSettingsViewModel(
        getSettings = GetMailboxSettingsUseCase(repository),
        updateSettings = UpdateMailboxSettingsUseCase(repository),
        mailboxId = "mb1",
    )

    // --- loading ---------------------------------------------------------------------------

    @Test
    fun `loads the mailbox settings into the form and starts clean`() = runTest {
        val viewModel = buildViewModel()

        val state = viewModel.state.value
        assertFalse(state.loading)
        assertNull(state.loadError)
        assertEquals(initial, state.form)
        assertFalse(state.dirty)
    }

    @Test
    fun `a failed load shows the error, and retry loads again`() = runTest {
        val repository = repository(settings = Result.failure(RuntimeException("offline")))
        val viewModel = buildViewModel(repository)
        assertEquals("offline", viewModel.state.value.loadError)
        assertFalse(viewModel.state.value.loading)

        repository.settingsResult = Result.success(initial)
        viewModel.retry()

        assertNull(viewModel.state.value.loadError)
        assertEquals(initial, viewModel.state.value.form)
        assertEquals(2, repository.settingsLoads)
    }

    // --- editing -----------------------------------------------------------------------------

    @Test
    fun `each field edit updates the form and marks it dirty`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onFromNameChanged("Alicia")
        viewModel.onSignatureEnabledChanged(false)
        viewModel.onSignatureTextChanged("Best")

        assertEquals(MailboxSettings("Alicia", false, "Best"), viewModel.state.value.form)
        assertTrue(viewModel.state.value.dirty)
    }

    @Test
    fun `editing back to the saved values is not dirty`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onFromNameChanged("Alicia")
        viewModel.onFromNameChanged("Alice")

        assertFalse(viewModel.state.value.dirty)
    }

    // --- saving ------------------------------------------------------------------------------

    @Test
    fun `save sends the edited settings, then reports success and goes clean`() = runTest {
        val repository = repository()
        val viewModel = buildViewModel(repository)
        viewModel.onSignatureTextChanged("Best")

        viewModel.save()

        assertEquals(listOf(initial.copy(signatureText = "Best")), repository.updateSettingsCalls)
        val state = viewModel.state.value
        assertFalse(state.saving)
        assertFalse(state.dirty)
        assertEquals("Settings saved", state.message)
    }

    @Test
    fun `the display name is trimmed on save`() = runTest {
        val repository = repository()
        val viewModel = buildViewModel(repository)
        viewModel.onFromNameChanged("  Alicia  ")

        viewModel.save()

        assertEquals("Alicia", repository.updateSettingsCalls.single().fromName)
        assertEquals("Alicia", viewModel.state.value.form.fromName)
    }

    @Test
    fun `a failed save keeps the edits and stays dirty so the user can retry`() = runTest {
        val repository = repository()
        repository.updateSettingsResult = { Result.failure(ApiException(429, "Too many requests — please try again shortly")) }
        val viewModel = buildViewModel(repository)
        viewModel.onFromNameChanged("Alicia")

        viewModel.save()

        val state = viewModel.state.value
        assertEquals("Alicia", state.form.fromName)
        assertTrue(state.dirty)
        assertFalse(state.saving)
        assertEquals("Too many requests — please try again shortly", state.message)
    }

    @Test
    fun `save does nothing when there is nothing to save`() = runTest {
        val repository = repository()
        val viewModel = buildViewModel(repository)

        viewModel.save()

        assertTrue(repository.updateSettingsCalls.isEmpty())
    }

    @Test
    fun `a second save while one is in flight is ignored`() = runTest {
        val repository = repository()
        repository.updateSettingsGate = CompletableDeferred()
        val viewModel = buildViewModel(repository)
        viewModel.onFromNameChanged("Alicia")

        viewModel.save()
        assertTrue(viewModel.state.value.saving)
        viewModel.save()

        assertEquals(1, repository.updateSettingsCalls.size)
        repository.updateSettingsGate!!.complete(Unit)
    }

    @Test
    fun `consumeMessage clears the snackbar message`() = runTest {
        val viewModel = buildViewModel()
        viewModel.onFromNameChanged("Alicia")
        viewModel.save()
        assertTrue(viewModel.state.value.message != null)

        viewModel.consumeMessage()

        assertNull(viewModel.state.value.message)
    }
}
