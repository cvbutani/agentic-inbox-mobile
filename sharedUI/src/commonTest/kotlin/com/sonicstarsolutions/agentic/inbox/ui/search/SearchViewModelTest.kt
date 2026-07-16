package com.sonicstarsolutions.agentic.inbox.ui.search

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.domain.model.SearchQuery
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SearchEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.testutil.FakeEmailRepository
import kotlinx.coroutines.CompletableDeferred
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
        SearchViewModel(searchEmails = SearchEmailsUseCase(repository), mailboxId = "mb1")

    @Test
    fun `initial state is empty and not loading`() = runTest {
        val viewModel = buildViewModel()

        assertFalse(viewModel.state.value.loading)
        assertFalse(viewModel.state.value.hasSearched)
        assertTrue(viewModel.state.value.results.isEmpty())
    }

    @Test
    fun `search rejects an empty query with no filters set`() = runTest {
        val repository = FakeEmailRepository()
        val viewModel = buildViewModel(repository)

        viewModel.search()

        assertTrue(repository.searchCalls.isEmpty())
        assertEquals("Enter a search term or a filter.", viewModel.state.value.errorMessage)
    }

    @Test
    fun `search proceeds with only a filter set and no query text`() = runTest {
        val repository = FakeEmailRepository()
        val viewModel = buildViewModel(repository)
        viewModel.onStarredFilterChanged(true)

        viewModel.search()

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
    fun `search surfaces the failure message`() = runTest {
        val repository = FakeEmailRepository(searchHandler = { _, _, _, _ -> Result.failure(RuntimeException("server down")) })
        val viewModel = buildViewModel(repository)
        viewModel.onQueryChanged("invoice")

        viewModel.search()

        assertEquals("server down", viewModel.state.value.errorMessage)
        assertTrue(viewModel.state.value.hasSearched)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun `search ignores a second call while one is already in flight`() = runTest {
        val repository = FakeEmailRepository()
        val viewModel = buildViewModel(repository)
        viewModel.onQueryChanged("invoice")
        val gate = CompletableDeferred<Unit>()
        repository.searchGate = gate

        viewModel.search()
        assertTrue(viewModel.state.value.loading)

        viewModel.search() // should be ignored by the loading guard

        assertEquals(1, repository.searchCalls.size, "second search should not have dispatched a request")

        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `loadMore appends the next page and advances the page counter`() = runTest {
        val repository = FakeEmailRepository(
            searchHandler = { _, _, page, _ -> Result.success(EmailPage(listOf(summary("e$page")), totalCount = 3)) },
        )
        val viewModel = buildViewModel(repository)
        viewModel.onQueryChanged("invoice")
        viewModel.search() // page 1 -> [e1], totalCount 3

        viewModel.loadMore()

        assertEquals(listOf(summary("e1"), summary("e2")), viewModel.state.value.results)
        assertEquals(2, viewModel.state.value.page)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun `loadMore does nothing once every result has been loaded`() = runTest {
        val repository = FakeEmailRepository(
            searchHandler = { _, _, _, _ -> Result.success(EmailPage(listOf(summary("e1")), totalCount = 1)) },
        )
        val viewModel = buildViewModel(repository)
        viewModel.onQueryChanged("invoice")
        viewModel.search() // fully loaded: 1 of 1

        viewModel.loadMore()

        assertEquals(1, repository.searchCalls.size, "loadMore should not have issued a second request")
        assertEquals(1, viewModel.state.value.page)
    }

    @Test
    fun `consumeError clears the error message`() = runTest {
        val viewModel = buildViewModel()
        viewModel.search() // empty criteria -> sets an error
        assertTrue(viewModel.state.value.errorMessage != null)

        viewModel.consumeError()

        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `clearFilters resets all filter fields but keeps the query text`() = runTest {
        val viewModel = buildViewModel()
        viewModel.onQueryChanged("invoice")
        viewModel.onFromChanged("alice@example.dev")
        viewModel.onStarredFilterChanged(true)

        viewModel.clearFilters()

        assertEquals("invoice", viewModel.state.value.queryText)
        assertEquals("", viewModel.state.value.from)
        assertNull(viewModel.state.value.isStarred)
    }
}
