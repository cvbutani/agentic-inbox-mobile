package com.sonicstarsolutions.agentic.inbox.ui.search

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.domain.model.SearchQuery
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SearchEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SetEmailStarredUseCase
import com.sonicstarsolutions.agentic.inbox.testutil.FakeEmailRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun summary(id: String) = EmailSummary(
        id = id,
        subject = "Subject $id",
        sender = "a@example.dev",
        recipient = "b@example.dev",
        date = "2026-07-16T00:00:00Z",
        read = false,
        starred = false,
        threadId = null,
        folderId = "inbox",
        snippet = null,
    )

    private fun buildViewModel(repository: FakeEmailRepository = FakeEmailRepository()): SearchViewModel =
        SearchViewModel(
            searchEmails = SearchEmailsUseCase(repository),
            setEmailStarred = SetEmailStarredUseCase(repository),
            mailboxId = "mb1",
        )

    @Test
    fun `initial state is empty and not loading`() = runTest {
        val viewModel = buildViewModel()

        assertFalse(viewModel.state.value.loading)
        assertFalse(viewModel.state.value.hasSearched)
        assertTrue(viewModel.state.value.results.isEmpty())
    }

    // -- Debounced live search --------------------------------------------------------------

    @Test
    fun `typing triggers a search automatically after the debounce`() = runTest {
        val repository = FakeEmailRepository(
            searchHandler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1")), 1)) },
        )
        val viewModel = buildViewModel(repository)

        viewModel.onQueryChanged("invoice")
        assertTrue(repository.searchCalls.isEmpty(), "search must not fire before the debounce")

        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 50)
        advanceUntilIdle()

        assertEquals(1, repository.searchCalls.size)
        assertEquals("invoice", repository.searchCalls.single().query.query)
        assertEquals(listOf("e1"), viewModel.state.value.results.map { it.id })
        assertTrue(viewModel.state.value.hasSearched)
    }

    @Test
    fun `rapid typing coalesces into one search for the final text`() = runTest {
        val repository = FakeEmailRepository()
        val viewModel = buildViewModel(repository)

        viewModel.onQueryChanged("i")
        advanceTimeBy(100)
        viewModel.onQueryChanged("in")
        advanceTimeBy(100)
        viewModel.onQueryChanged("inv")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 50)
        advanceUntilIdle()

        assertEquals(1, repository.searchCalls.size)
        assertEquals("inv", repository.searchCalls.single().query.query)
    }

    @Test
    fun `a newer search cancels the in-flight one so stale results never land`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeEmailRepository(
            searchHandler = { _, query, _, _ ->
                Result.success(EmailPage(listOf(summary("result-for-${query.query}")), 1))
            },
        ).apply { searchGate = gate }
        val viewModel = buildViewModel(repository)

        viewModel.onQueryChanged("first")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 50)
        assertEquals(1, repository.searchCalls.size, "first search should be in flight")

        viewModel.onQueryChanged("second")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 50)

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(
            listOf("result-for-second"),
            viewModel.state.value.results.map { it.id },
            "only the newest query's results may land",
        )
    }

    @Test
    fun `clearing the query clears results without an error and without a request`() = runTest {
        val repository = FakeEmailRepository(
            searchHandler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1")), 1)) },
        )
        val viewModel = buildViewModel(repository)

        viewModel.onQueryChanged("invoice")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 50)
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.results.size)

        viewModel.onQueryChanged("")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 50)
        advanceUntilIdle()

        assertTrue(viewModel.state.value.results.isEmpty())
        assertFalse(viewModel.state.value.hasSearched)
        assertNull(viewModel.state.value.errorMessage)
        assertEquals(1, repository.searchCalls.size, "a blank query must not hit the backend")
    }

    @Test
    fun `an explicit blank search with no filters clears silently instead of erroring`() = runTest {
        val repository = FakeEmailRepository()
        val viewModel = buildViewModel(repository)

        viewModel.search()

        assertTrue(repository.searchCalls.isEmpty())
        assertNull(viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.hasSearched)
    }

    // -- Explicit search and filters --------------------------------------------------------

    @Test
    fun `search proceeds with only a filter set and no query text`() = runTest {
        val repository = FakeEmailRepository()
        val viewModel = buildViewModel(repository)
        viewModel.onStarredFilterChanged(true)

        viewModel.search()
        advanceUntilIdle()

        assertEquals(1, repository.searchCalls.size)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `search trims the query text and executes with the composed query`() = runTest {
        val repository = FakeEmailRepository(
            searchHandler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1"), summary("e2")), totalCount = 2)) },
        )
        val viewModel = buildViewModel(repository)
        viewModel.onQueryChanged("  invoice  ")

        viewModel.search()
        advanceUntilIdle()

        val call = repository.searchCalls.single()
        assertEquals("mb1", call.mailboxId)
        assertEquals("invoice", call.query.query)
        assertEquals(1, call.page)
        assertEquals(listOf(summary("e1"), summary("e2")), viewModel.state.value.results)
        assertEquals(2, viewModel.state.value.totalCount)
        assertTrue(viewModel.state.value.hasSearched)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun `search includes active filters in the composed query`() = runTest {
        val repository = FakeEmailRepository()
        val viewModel = buildViewModel(repository)
        viewModel.onQueryChanged("invoice")
        viewModel.onFromChanged("alice@example.dev")
        viewModel.onToChanged("me@example.dev")
        viewModel.onSubjectChanged("receipt")
        viewModel.onDateStartChanged("2026-01-01")
        viewModel.onDateEndChanged("2026-07-01")
        viewModel.onReadFilterChanged(false)
        viewModel.onStarredFilterChanged(true)
        viewModel.onAttachmentFilterChanged(true)

        viewModel.search()
        advanceUntilIdle()

        val query = repository.searchCalls.single().query
        assertEquals(
            SearchQuery(
                query = "invoice",
                from = "alice@example.dev",
                to = "me@example.dev",
                subject = "receipt",
                dateStart = "2026-01-01",
                dateEnd = "2026-07-01",
                isRead = false,
                isStarred = true,
                hasAttachment = true,
            ),
            query,
        )
    }

    @Test
    fun `an explicit search restarts an in-flight one rather than being swallowed`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeEmailRepository(
            searchHandler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1")), 1)) },
        ).apply { searchGate = gate }
        val viewModel = buildViewModel(repository)
        viewModel.onQueryChanged("invoice")

        viewModel.search()
        assertTrue(viewModel.state.value.loading)
        viewModel.search()

        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(2, repository.searchCalls.size, "the second explicit search replaces the first")
        assertEquals(listOf("e1"), viewModel.state.value.results.map { it.id })
        assertFalse(viewModel.state.value.loading)
    }

    // -- Failures ---------------------------------------------------------------------------

    @Test
    fun `search surfaces the failure message`() = runTest {
        val repository = FakeEmailRepository(searchHandler = { _, _, _, _ -> Result.failure(RuntimeException("server down")) })
        val viewModel = buildViewModel(repository)
        viewModel.onQueryChanged("invoice")

        viewModel.search()
        advanceUntilIdle()

        assertEquals("server down", viewModel.state.value.errorMessage)
        assertTrue(viewModel.state.value.hasSearched)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun `a failed search keeps the previous results and reports the error`() = runTest {
        var fail = false
        val repository = FakeEmailRepository(
            searchHandler = { _, _, _, _ ->
                if (fail) Result.failure(RuntimeException("boom"))
                else Result.success(EmailPage(listOf(summary("e1"), summary("e2")), 2))
            },
        )
        val viewModel = buildViewModel(repository)

        viewModel.onQueryChanged("alpha")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 50)
        advanceUntilIdle()
        assertEquals(2, viewModel.state.value.results.size)

        fail = true
        viewModel.onQueryChanged("alphabet")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 50)
        advanceUntilIdle()

        assertEquals("boom", viewModel.state.value.errorMessage)
        assertEquals(2, viewModel.state.value.results.size, "an error must not blank out what the user can see")
    }

    @Test
    fun `consumeError clears the error message`() = runTest {
        val repository = FakeEmailRepository(searchHandler = { _, _, _, _ -> Result.failure(RuntimeException("boom")) })
        val viewModel = buildViewModel(repository)
        viewModel.onQueryChanged("invoice")
        viewModel.search()
        advanceUntilIdle()
        assertNotNull(viewModel.state.value.errorMessage)

        viewModel.consumeError()

        assertNull(viewModel.state.value.errorMessage)
    }

    // -- Pagination -------------------------------------------------------------------------

    @Test
    fun `loadMore appends the next page and advances the page counter`() = runTest {
        val repository = FakeEmailRepository(
            searchHandler = { _, _, page, _ -> Result.success(EmailPage(listOf(summary("e$page")), totalCount = 3)) },
        )
        val viewModel = buildViewModel(repository)
        viewModel.onQueryChanged("invoice")
        viewModel.search() // page 1 -> [e1], totalCount 3
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf(summary("e1"), summary("e2")), viewModel.state.value.results)
        assertEquals(2, viewModel.state.value.page)
        assertFalse(viewModel.state.value.loadingMore)
    }

    @Test
    fun `loadMore does nothing once every result has been loaded`() = runTest {
        val repository = FakeEmailRepository(
            searchHandler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1")), totalCount = 1)) },
        )
        val viewModel = buildViewModel(repository)
        viewModel.onQueryChanged("invoice")
        viewModel.search() // fully loaded: 1 of 1
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(1, repository.searchCalls.size, "loadMore should not have issued a second request")
        assertEquals(1, viewModel.state.value.page)
    }

    @Test
    fun `load more fetches the next page of the submitted query not the draft text`() = runTest {
        val repository = FakeEmailRepository(
            searchHandler = { _, _, page, _ ->
                Result.success(EmailPage(listOf(summary("p$page-a"), summary("p$page-b")), 4))
            },
        )
        val viewModel = buildViewModel(repository)

        viewModel.onQueryChanged("alpha")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 50)
        advanceUntilIdle()
        assertEquals(2, viewModel.state.value.results.size)

        // The user has typed new text but its debounce hasn't fired yet.
        viewModel.onQueryChanged("beta")
        viewModel.loadMore()

        val pageTwoCall = repository.searchCalls[1]
        assertEquals("alpha", pageTwoCall.query.query, "page 2 must belong to the query that produced page 1")
        assertEquals(2, pageTwoCall.page)
        assertEquals(4, viewModel.state.value.results.size)
    }

    @Test
    fun `load more drops duplicate ids so the list never holds two rows with one key`() = runTest {
        val repository = FakeEmailRepository(
            searchHandler = { _, _, page, _ ->
                // The backend shifted between pages: "e2" appears on both.
                if (page == 1) Result.success(EmailPage(listOf(summary("e1"), summary("e2")), 3))
                else Result.success(EmailPage(listOf(summary("e2"), summary("e3")), 3))
            },
        )
        val viewModel = buildViewModel(repository)

        viewModel.onQueryChanged("alpha")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 50)
        advanceUntilIdle()
        viewModel.loadMore()
        advanceUntilIdle()

        assertEquals(listOf("e1", "e2", "e3"), viewModel.state.value.results.map { it.id })
    }

    // -- Date validation --------------------------------------------------------------------

    @Test
    fun `an invalid date format flags the field and blocks the search`() = runTest {
        val repository = FakeEmailRepository()
        val viewModel = buildViewModel(repository)

        viewModel.onQueryChanged("alpha")
        viewModel.onDateStartChanged("17-07-2026")

        assertNotNull(viewModel.state.value.dateError)

        viewModel.search()
        advanceUntilIdle()
        assertTrue(repository.searchCalls.isEmpty(), "an invalid date must never reach the backend")
    }

    @Test
    fun `an end date before the start date is rejected`() = runTest {
        val repository = FakeEmailRepository()
        val viewModel = buildViewModel(repository)

        viewModel.onDateStartChanged("2026-07-17")
        viewModel.onDateEndChanged("2026-07-01")

        assertNotNull(viewModel.state.value.dateError)

        viewModel.search()
        advanceUntilIdle()
        assertTrue(repository.searchCalls.isEmpty())
    }

    @Test
    fun `fixing an invalid date clears the error`() = runTest {
        val viewModel = buildViewModel()

        viewModel.onDateStartChanged("17-07-2026")
        assertNotNull(viewModel.state.value.dateError)

        viewModel.onDateStartChanged("2026-07-17")
        assertNull(viewModel.state.value.dateError)
    }

    // -- Filters ----------------------------------------------------------------------------

    @Test
    fun `clearFilters resets all filter fields but keeps the query text`() = runTest {
        val viewModel = buildViewModel()
        viewModel.onQueryChanged("invoice")
        viewModel.onFromChanged("alice@example.dev")
        viewModel.onStarredFilterChanged(true)
        viewModel.onDateStartChanged("17-07-2026") // leaves a dateError behind

        viewModel.clearFilters()

        assertEquals("invoice", viewModel.state.value.queryText)
        assertEquals("", viewModel.state.value.from)
        assertNull(viewModel.state.value.isStarred)
        assertNull(viewModel.state.value.dateError)
    }

    // -- Starring ---------------------------------------------------------------------------

    @Test
    fun `toggling a star updates the row optimistically`() = runTest {
        val repository = FakeEmailRepository(
            searchHandler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1")), 1)) },
        )
        val viewModel = buildViewModel(repository)

        viewModel.onQueryChanged("alpha")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 50)
        advanceUntilIdle()

        viewModel.toggleStarred(viewModel.state.value.results.single())
        advanceUntilIdle()

        assertTrue(viewModel.state.value.results.single().starred)
        val starCall = repository.setStarredCalls.single()
        assertEquals("e1", starCall.emailId)
        assertTrue(starCall.starred)
    }

    @Test
    fun `a failed star toggle reverts the row and reports the error`() = runTest {
        val repository = FakeEmailRepository(
            searchHandler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1")), 1)) },
            setStarredResult = Result.failure(RuntimeException("offline")),
        )
        val viewModel = buildViewModel(repository)

        viewModel.onQueryChanged("alpha")
        advanceTimeBy(SearchViewModel.DEBOUNCE_MILLIS + 50)
        advanceUntilIdle()

        viewModel.toggleStarred(viewModel.state.value.results.single())
        advanceUntilIdle()

        assertFalse(viewModel.state.value.results.single().starred)
        assertEquals("offline", viewModel.state.value.errorMessage)
    }
}
