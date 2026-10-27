package com.sonicstarsolutions.agentic.inbox.ui.mailbox.agentaccess

import com.sonicstarsolutions.agentic.inbox.domain.model.AgentScope
import com.sonicstarsolutions.agentic.inbox.domain.model.FeatureUnavailableException
import com.sonicstarsolutions.agentic.inbox.domain.model.NewAgentKey
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CreateAgentKeyUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ListAgentKeysUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.RevokeAgentKeyUseCase
import com.sonicstarsolutions.agentic.inbox.testutil.FakeAgentKeyRepository
import com.sonicstarsolutions.agentic.inbox.testutil.agentKey
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
class AgentAccessViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(repository: FakeAgentKeyRepository = FakeAgentKeyRepository()) = AgentAccessViewModel(
        listKeys = ListAgentKeysUseCase(repository),
        createKey = CreateAgentKeyUseCase(repository),
        revokeKey = RevokeAgentKeyUseCase(repository),
        mailboxId = "mb1",
    )

    // --- loading -----------------------------------------------------------------------------

    @Test
    fun `loads the keys, active ones first, newest first within each group`() = runTest {
        val repository = FakeAgentKeyRepository(
            listOf(
                agentKey("old", createdAt = "2026-10-01T00:00:00.000Z"),
                agentKey("gone", createdAt = "2026-10-20T00:00:00.000Z", revokedAt = "2026-10-21T00:00:00.000Z"),
                agentKey("new", createdAt = "2026-10-25T00:00:00.000Z"),
            ),
        )

        val viewModel = buildViewModel(repository)

        assertFalse(viewModel.state.value.loading)
        assertEquals(listOf("new", "old", "gone"), viewModel.state.value.keys.map { it.id })
    }

    @Test
    fun `a failed load shows the error, and retry loads again`() = runTest {
        val repository = FakeAgentKeyRepository().apply { listFailure = RuntimeException("offline") }
        val viewModel = buildViewModel(repository)
        assertEquals("offline", viewModel.state.value.loadError)

        repository.listFailure = null
        repository.keys += agentKey("k1")
        viewModel.retry()

        assertNull(viewModel.state.value.loadError)
        assertEquals(listOf("k1"), viewModel.state.value.keys.map { it.id })
    }

    @Test
    fun `a worker without agent access explains itself instead of showing a raw error`() = runTest {
        val repository = FakeAgentKeyRepository().apply {
            listFailure = FeatureUnavailableException("Agent access isn't enabled on this server")
        }

        val viewModel = buildViewModel(repository)

        assertEquals("Agent access isn't enabled on this server", viewModel.state.value.loadError)
    }

    // --- creating ----------------------------------------------------------------------------

    @Test
    fun `creating a key sends the trimmed name and scopes, then reveals the secret once`() = runTest {
        val repository = FakeAgentKeyRepository()
        val viewModel = buildViewModel(repository)

        viewModel.create(name = "  Claude Code  ", scopes = setOf(AgentScope.READ, AgentScope.DRAFT), dailySendLimit = null)

        assertEquals(listOf(NewAgentKey("Claude Code", listOf(AgentScope.READ, AgentScope.DRAFT), null)), repository.createCalls)
        val state = viewModel.state.value
        assertEquals("Claude Code", state.createdKey?.key?.name)
        assertTrue(state.createdKey!!.secret.startsWith("agt_"))
        assertFalse(state.saving)
        // The new key is in the list without the user having to refresh.
        assertEquals(listOf("Claude Code"), state.keys.map { it.name })
    }

    @Test
    fun `scopes are sent in a stable read, draft, send order`() = runTest {
        val repository = FakeAgentKeyRepository()
        val viewModel = buildViewModel(repository)

        viewModel.create("k", setOf(AgentScope.SEND, AgentScope.READ), dailySendLimit = 3)

        assertEquals(listOf(AgentScope.READ, AgentScope.SEND), repository.createCalls.single().scopes)
    }

    @Test
    fun `a daily limit only travels with a key that can send`() = runTest {
        val repository = FakeAgentKeyRepository()
        val viewModel = buildViewModel(repository)

        viewModel.create("reader", setOf(AgentScope.READ), dailySendLimit = 9)
        viewModel.dismissSecret()
        viewModel.create("sender", setOf(AgentScope.SEND), dailySendLimit = 9)

        assertEquals(listOf(null, 9), repository.createCalls.map { it.dailySendLimit })
    }

    @Test
    fun `a blank name or no permission is refused before anything is sent`() = runTest {
        val repository = FakeAgentKeyRepository()
        val viewModel = buildViewModel(repository)

        viewModel.create("   ", setOf(AgentScope.READ), null)
        assertEquals("Name the key and pick at least one permission", viewModel.state.value.message)
        viewModel.consumeMessage()
        viewModel.create("k", emptySet(), null)
        assertEquals("Name the key and pick at least one permission", viewModel.state.value.message)

        assertTrue(repository.createCalls.isEmpty())
    }

    @Test
    fun `a daily limit below one is refused`() = runTest {
        val repository = FakeAgentKeyRepository()
        val viewModel = buildViewModel(repository)

        viewModel.create("k", setOf(AgentScope.SEND), dailySendLimit = 0)

        assertTrue(repository.createCalls.isEmpty())
        assertEquals("The daily send limit must be at least 1", viewModel.state.value.message)
    }

    @Test
    fun `a failed create reports the error and shows no secret`() = runTest {
        val repository = FakeAgentKeyRepository().apply { createFailure = RuntimeException("offline") }
        val viewModel = buildViewModel(repository)

        viewModel.create("k", setOf(AgentScope.READ), null)

        assertEquals("offline", viewModel.state.value.message)
        assertNull(viewModel.state.value.createdKey)
        assertFalse(viewModel.state.value.saving)
    }

    @Test
    fun `a second create while one is in flight is ignored`() = runTest {
        val repository = FakeAgentKeyRepository().apply { createGate = CompletableDeferred() }
        val viewModel = buildViewModel(repository)

        viewModel.create("k", setOf(AgentScope.READ), null)
        assertTrue(viewModel.state.value.saving)
        viewModel.create("k", setOf(AgentScope.READ), null)

        assertEquals(1, repository.createCalls.size)
        repository.createGate!!.complete(Unit)
    }

    @Test
    fun `dismissing the secret clears it for good`() = runTest {
        val viewModel = buildViewModel()
        viewModel.create("k", setOf(AgentScope.READ), null)
        assertTrue(viewModel.state.value.createdKey != null)

        viewModel.dismissSecret()

        assertNull(viewModel.state.value.createdKey)
    }

    // --- revoking ----------------------------------------------------------------------------

    @Test
    fun `revoking a key marks it revoked in the list`() = runTest {
        val repository = FakeAgentKeyRepository(listOf(agentKey("k1"), agentKey("k2", createdAt = "2026-10-26T00:00:00.000Z")))
        val viewModel = buildViewModel(repository)

        viewModel.revoke("k1")

        assertEquals(listOf("k1"), repository.revokeCalls)
        assertEquals(listOf("k2", "k1"), viewModel.state.value.keys.map { it.id })
        assertTrue(viewModel.state.value.keys.last().revoked)
        assertEquals("Key revoked", viewModel.state.value.message)
    }

    @Test
    fun `a failed revoke leaves the key active and reports the error`() = runTest {
        val repository = FakeAgentKeyRepository(listOf(agentKey("k1"))).apply { revokeFailure = RuntimeException("offline") }
        val viewModel = buildViewModel(repository)

        viewModel.revoke("k1")

        assertFalse(viewModel.state.value.keys.single().revoked)
        assertEquals("offline", viewModel.state.value.message)
        assertFalse(viewModel.state.value.saving)
    }

    @Test
    fun `consumeMessage clears the snackbar message`() = runTest {
        val viewModel = buildViewModel(FakeAgentKeyRepository(listOf(agentKey("k1"))))
        viewModel.revoke("k1")
        assertTrue(viewModel.state.value.message != null)

        viewModel.consumeMessage()

        assertNull(viewModel.state.value.message)
    }
}
