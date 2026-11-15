package com.sonicstarsolutions.agentic.inbox.ui.scheduled

import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduleKind
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduledEmail
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduledStatus
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CancelScheduledEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetScheduledEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ObserveLiveUpdatesUseCase
import com.sonicstarsolutions.agentic.inbox.domain.model.LiveScope
import com.sonicstarsolutions.agentic.inbox.testutil.FakeLiveUpdates
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import com.sonicstarsolutions.agentic.inbox.testutil.FakeScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ScheduledViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun item(id: String, status: ScheduledStatus = ScheduledStatus.PENDING) = ScheduledEmail(
        id = id,
        kind = ScheduleKind.COMPOSE,
        sendAt = "2026-11-19T08:00:00.000Z",
        status = status,
        error = null,
        to = "alice@example.dev",
        subject = "Subject $id",
    )

    private fun buildViewModel(repository: FakeScheduleRepository, live: FakeLiveUpdates = FakeLiveUpdates()) = ScheduledViewModel(
        getScheduled = GetScheduledEmailsUseCase(repository),
        cancelScheduled = CancelScheduledEmailUseCase(repository),
        observeLiveUpdates = ObserveLiveUpdatesUseCase(live),
        mailboxId = "mb1",
    )

    @Test
    fun `the queue loads on open`() = runTest {
        val viewModel = buildViewModel(FakeScheduleRepository(scheduledResult = Result.success(listOf(item("a"), item("b")))))

        val state = viewModel.state.value
        assertFalse(state.loading)
        assertEquals(listOf("a", "b"), state.items.map { it.id })
        assertNull(state.loadError)
    }

    @Test
    fun `an empty queue is not an error`() = runTest {
        val viewModel = buildViewModel(FakeScheduleRepository())

        assertTrue(viewModel.state.value.items.isEmpty())
        assertNull(viewModel.state.value.loadError)
    }

    @Test
    fun `a failed load shows the error and retry loads again`() = runTest {
        val repository = FakeScheduleRepository(scheduledResult = Result.failure(RuntimeException("offline")))
        val viewModel = buildViewModel(repository)
        assertEquals("offline", viewModel.state.value.loadError)

        repository.scheduledResult = Result.success(listOf(item("a")))
        viewModel.retry()

        assertNull(viewModel.state.value.loadError)
        assertEquals(listOf("a"), viewModel.state.value.items.map { it.id })
        assertEquals(2, repository.getScheduledCalls)
    }

    @Test
    fun `cancelling removes the message and says so`() = runTest {
        val repository = FakeScheduleRepository(scheduledResult = Result.success(listOf(item("a"), item("b"))))
        val viewModel = buildViewModel(repository)

        viewModel.cancel(item("a"))

        assertEquals(listOf("a"), repository.cancelCalls)
        assertEquals(listOf("b"), viewModel.state.value.items.map { it.id })
        assertEquals("Scheduled message cancelled", viewModel.state.value.message)
    }

    @Test
    fun `a cancel the server refuses keeps the message, explains, and refreshes the list`() = runTest {
        val repository = FakeScheduleRepository(
            scheduledResult = Result.success(listOf(item("a"))),
            cancelResult = Result.failure(RuntimeException("Not found, or already sending")),
        )
        val viewModel = buildViewModel(repository)
        // By the time the user taps, the server has moved on: it is sending now.
        repository.scheduledResult = Result.success(listOf(item("a", ScheduledStatus.SENDING)))

        viewModel.cancel(item("a"))
        advanceUntilIdle()

        assertEquals("Not found, or already sending", viewModel.state.value.message)
        assertEquals(ScheduledStatus.SENDING, viewModel.state.value.items.single().status)
    }

    @Test
    fun `consumeMessage clears the snackbar line`() = runTest {
        val viewModel = buildViewModel(FakeScheduleRepository(scheduledResult = Result.success(listOf(item("a")))))
        viewModel.cancel(item("a"))

        viewModel.consumeMessage()

        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `refresh keeps the current rows visible while it reloads`() = runTest {
        val repository = FakeScheduleRepository(scheduledResult = Result.success(listOf(item("a"))))
        val viewModel = buildViewModel(repository)
        repository.scheduledResult = Result.success(listOf(item("a"), item("b")))

        viewModel.refresh()

        assertEquals(listOf("a", "b"), viewModel.state.value.items.map { it.id })
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun `a change to the scheduled queue reloads it`() = runTest {
        val repository = FakeScheduleRepository(scheduledResult = Result.success(listOf(item("a"))))
        val live = FakeLiveUpdates()
        val viewModel = buildViewModel(repository, live)
        backgroundScope.launch { viewModel.keepLive() }
        advanceTimeBy(1)
        repository.scheduledResult = Result.success(listOf(item("a"), item("b")))

        live.emit(LiveScope.SCHEDULED)
        advanceTimeBy(ScheduledViewModel.LIVE_DEBOUNCE_MILLIS + 1)

        assertEquals(listOf("a", "b"), viewModel.state.value.items.map { it.id })
    }

    @Test
    fun `a change that is only about emails or folders is ignored`() = runTest {
        val repository = FakeScheduleRepository()
        val live = FakeLiveUpdates()
        val viewModel = buildViewModel(repository, live)
        backgroundScope.launch { viewModel.keepLive() }
        advanceTimeBy(1)
        val before = repository.getScheduledCalls

        live.emit(LiveScope.EMAILS)
        live.emit(LiveScope.FOLDERS)
        advanceTimeBy(ScheduledViewModel.LIVE_DEBOUNCE_MILLIS + 1)

        assertEquals(before, repository.getScheduledCalls)
    }

    @Test
    fun `an all-purpose notice reloads the queue too`() = runTest {
        val repository = FakeScheduleRepository()
        val live = FakeLiveUpdates()
        val viewModel = buildViewModel(repository, live)
        backgroundScope.launch { viewModel.keepLive() }
        advanceTimeBy(1)
        val before = repository.getScheduledCalls

        live.emit(LiveScope.ALL)
        advanceTimeBy(ScheduledViewModel.LIVE_DEBOUNCE_MILLIS + 1)

        assertEquals(before + 1, repository.getScheduledCalls)
    }
}
