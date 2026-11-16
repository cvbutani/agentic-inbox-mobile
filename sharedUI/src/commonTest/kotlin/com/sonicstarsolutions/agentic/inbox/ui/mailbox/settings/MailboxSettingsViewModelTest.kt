package com.sonicstarsolutions.agentic.inbox.ui.mailbox.settings

import com.sonicstarsolutions.agentic.inbox.data.network.ApiException
import com.sonicstarsolutions.agentic.inbox.domain.model.DomainSettings
import com.sonicstarsolutions.agentic.inbox.domain.model.MailboxSettings
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetDomainSettingsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxSettingsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.UpdateDomainSettingsUseCase
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

    private fun buildViewModel(
        repository: FakeMailboxRepository = repository(),
        mailboxId: String = "mb1",
    ) = MailboxSettingsViewModel(
        getSettings = GetMailboxSettingsUseCase(repository),
        updateSettings = UpdateMailboxSettingsUseCase(repository),
        getDomainSettings = GetDomainSettingsUseCase(repository),
        updateDomainSettings = UpdateDomainSettingsUseCase(repository),
        mailboxId = mailboxId,
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

    // --- aliases ---------------------------------------------------------------------------

    @Test
    fun `adding an alias normalises it, lists it and marks the form dirty`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onAliasAdded("  Sales@Example.DEV ")

        assertEquals(listOf("sales@example.dev"), viewModel.state.value.form.aliases)
        assertNull(viewModel.state.value.aliasError)
        assertTrue(viewModel.state.value.dirty)
    }

    @Test
    fun `an alias that is not an email address is refused with a reason`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onAliasAdded("not an email")

        assertTrue(viewModel.state.value.form.aliases.isEmpty())
        assertEquals("“not an email” isn't a valid email address.", viewModel.state.value.aliasError)
        assertFalse(viewModel.state.value.dirty)
    }

    @Test
    fun `an alias already in the list is refused`() = runTest {
        val viewModel = buildViewModel()
        viewModel.onAliasAdded("sales@example.dev")

        viewModel.onAliasAdded("SALES@example.dev")

        assertEquals(listOf("sales@example.dev"), viewModel.state.value.form.aliases)
        assertEquals("sales@example.dev is already an alias.", viewModel.state.value.aliasError)
    }

    @Test
    fun `the mailbox's own address cannot be its alias`() = runTest {
        val viewModel = buildViewModel(mailboxId = "me@example.dev")

        viewModel.onAliasAdded("Me@Example.dev")

        assertTrue(viewModel.state.value.form.aliases.isEmpty())
        assertEquals("An alias can't be the mailbox's own address.", viewModel.state.value.aliasError)
    }

    @Test
    fun `no more than twenty aliases are accepted`() = runTest {
        val viewModel = buildViewModel()
        repeat(20) { viewModel.onAliasAdded("a$it@example.dev") }

        viewModel.onAliasAdded("one-too-many@example.dev")

        assertEquals(20, viewModel.state.value.form.aliases.size)
        assertEquals("A mailbox can have at most 20 aliases.", viewModel.state.value.aliasError)
    }

    @Test
    fun `a good alias clears an earlier error`() = runTest {
        val viewModel = buildViewModel()
        viewModel.onAliasAdded("nope")

        viewModel.onAliasAdded("sales@example.dev")

        assertNull(viewModel.state.value.aliasError)
    }

    @Test
    fun `removing an alias takes it out of the form`() = runTest {
        val viewModel = buildViewModel(repository(Result.success(initial.copy(aliases = listOf("a@example.dev", "b@example.dev")))))

        viewModel.onAliasRemoved("a@example.dev")

        assertEquals(listOf("b@example.dev"), viewModel.state.value.form.aliases)
        assertTrue(viewModel.state.value.dirty)
    }

    @Test
    fun `saving sends the aliases`() = runTest {
        val repository = repository()
        val viewModel = buildViewModel(repository)
        viewModel.onAliasAdded("sales@example.dev")

        viewModel.save()

        assertEquals(listOf("sales@example.dev"), repository.updateSettingsCalls.single().aliases)
    }

    // --- auto-delete (retention) ---------------------------------------------------------------

    private val alice = "alice@example.dev"

    @Test
    fun `the domain's default retention is loaded beside the mailbox settings`() = runTest {
        val repository = repository()
        repository.domainSettingsResult = Result.success(DomainSettings("example.dev", 90))

        val viewModel = buildViewModel(repository, mailboxId = alice)

        assertEquals(listOf("example.dev"), repository.domainSettingsCalls)
        assertEquals(90, viewModel.state.value.domainRetentionDays)
    }

    @Test
    fun `a mailbox id without a domain asks for no domain settings`() = runTest {
        val repository = repository()

        buildViewModel(repository, mailboxId = "mb1")

        assertTrue(repository.domainSettingsCalls.isEmpty())
    }

    @Test
    fun `failing to load the domain default does not fail the screen`() = runTest {
        val repository = repository()
        repository.domainSettingsResult = Result.failure(RuntimeException("offline"))

        val viewModel = buildViewModel(repository, mailboxId = alice)

        assertNull(viewModel.state.value.loadError)
        assertNull(viewModel.state.value.domainRetentionDays)
    }

    @Test
    fun `choosing a retention for the mailbox is a dirty edit that save sends`() = runTest {
        val repository = repository()
        val viewModel = buildViewModel(repository, mailboxId = alice)

        viewModel.onRetentionChanged(7)
        assertTrue(viewModel.state.value.dirty)
        viewModel.save()

        assertEquals(7, repository.updateSettingsCalls.single().retentionDays)
    }

    @Test
    fun `going back to the domain default saves a null retention`() = runTest {
        val repository = repository(settings = Result.success(initial.copy(retentionDays = 7)))
        val viewModel = buildViewModel(repository, mailboxId = alice)

        viewModel.onRetentionChanged(null)
        viewModel.save()

        assertNull(repository.updateSettingsCalls.single().retentionDays)
    }

    @Test
    fun `the effective retention is the mailbox's own then the domain's then 30`() = runTest {
        val repository = repository()
        repository.domainSettingsResult = Result.success(DomainSettings("example.dev", 90))
        val viewModel = buildViewModel(repository, mailboxId = alice)

        assertEquals(90, viewModel.state.value.effectiveRetentionDays)
        viewModel.onRetentionChanged(7)
        assertEquals(7, viewModel.state.value.effectiveRetentionDays)
        viewModel.onRetentionChanged(0)
        assertEquals(0, viewModel.state.value.effectiveRetentionDays)
    }

    @Test
    fun `the effective retention is 30 days until anything says otherwise`() = runTest {
        val repository = repository()
        repository.domainSettingsResult = Result.failure(RuntimeException("offline"))
        val viewModel = buildViewModel(repository, mailboxId = alice)

        assertEquals(30, viewModel.state.value.effectiveRetentionDays)
    }

    @Test
    fun `choosing a domain default saves it straight away`() = runTest {
        val repository = repository()
        val viewModel = buildViewModel(repository, mailboxId = alice)

        viewModel.onDomainRetentionChosen(14)

        assertEquals(listOf("example.dev" to 14), repository.updateDomainCalls)
        assertEquals(14, viewModel.state.value.domainRetentionDays)
        assertEquals("Default for example.dev saved", viewModel.state.value.message)
        assertFalse(viewModel.state.value.dirty, "the domain default is not part of the mailbox form")
    }

    @Test
    fun `a failed domain default save keeps the old default and says so`() = runTest {
        val repository = repository()
        repository.updateDomainResult = { _, _ -> Result.failure(RuntimeException("boom")) }
        val viewModel = buildViewModel(repository, mailboxId = alice)

        viewModel.onDomainRetentionChosen(14)

        assertEquals(30, viewModel.state.value.domainRetentionDays)
        assertEquals("boom", viewModel.state.value.message)
    }
}
