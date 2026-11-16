package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.LiveScope
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.repository.LiveUpdates
import com.sonicstarsolutions.agentic.inbox.testutil.FakeMailboxRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveDomainLiveUpdatesUseCaseTest {

    private class PerMailboxLiveUpdates : LiveUpdates {
        val flows = mutableMapOf<String, MutableSharedFlow<LiveScope>>()
        val opened = mutableListOf<String>()

        override fun changes(mailboxId: String): Flow<LiveScope> =
            flows.getOrPut(mailboxId) { MutableSharedFlow(extraBufferCapacity = 16) }.onStart { opened += mailboxId }
    }

    private fun mailbox(email: String) = Mailbox(id = email, email = email, name = email)

    private fun useCase(live: PerMailboxLiveUpdates, vararg emails: String) = ObserveDomainLiveUpdatesUseCase(
        GetMailboxesUseCase(FakeMailboxRepository(result = Result.success(emails.map(::mailbox)))),
        ObserveLiveUpdatesUseCase(live),
    )

    @Test
    fun `listens to every mailbox on the domain and none on others`() = runTest(UnconfinedTestDispatcher()) {
        val live = PerMailboxLiveUpdates()
        val received = mutableListOf<LiveScope>()
        val job = launch {
            useCase(live, "a@example.com", "b@example.com", "c@other.org")("example.com").take(2).toList(received)
        }

        live.flows.getValue("a@example.com").emit(LiveScope.EMAILS)
        live.flows.getValue("b@example.com").emit(LiveScope.FOLDERS)
        job.join()

        assertEquals(listOf(LiveScope.EMAILS, LiveScope.FOLDERS), received)
        assertEquals(setOf("a@example.com", "b@example.com"), live.opened.toSet())
    }

    @Test
    fun `domain matching ignores case`() = runTest(UnconfinedTestDispatcher()) {
        val live = PerMailboxLiveUpdates()
        val job = launch { useCase(live, "a@Example.com")("example.COM").take(1).toList() }

        live.flows.getValue("a@Example.com").emit(LiveScope.EMAILS)
        job.join()

        assertEquals(listOf("a@Example.com"), live.opened)
    }

    @Test
    fun `opens at most eight connections`() = runTest(UnconfinedTestDispatcher()) {
        val live = PerMailboxLiveUpdates()
        val emails = (1..12).map { "m$it@example.com" }.toTypedArray()
        val job = launch { useCase(live, *emails)("example.com").take(1).toList() }

        live.flows.getValue("m1@example.com").emit(LiveScope.EMAILS)
        job.join()

        assertEquals(8, live.opened.size)
    }

    @Test
    fun `a domain with no mailboxes produces no notices and does not fail`() = runTest(UnconfinedTestDispatcher()) {
        val live = PerMailboxLiveUpdates()
        val received = useCase(live)("example.com").toList()
        assertEquals(emptyList(), received)
    }
}
