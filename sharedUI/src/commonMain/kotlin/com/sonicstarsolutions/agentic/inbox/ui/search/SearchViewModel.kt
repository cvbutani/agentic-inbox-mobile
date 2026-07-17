package com.sonicstarsolutions.agentic.inbox.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.domain.model.SearchQuery
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SearchEmailsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SetEmailStarredUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class SearchUiState(
    val queryText: String = "",
    val from: String = "",
    val to: String = "",
    val subject: String = "",
    val dateStart: String = "",
    val dateEnd: String = "",
    val isRead: Boolean? = null,
    val isStarred: Boolean? = null,
    val hasAttachment: Boolean? = null,
    /** Set while the date fields hold something that can't be sent — blocks searching. */
    val dateError: String? = null,
    /** First-page load in progress (skeletons); [loadingMore] covers appended pages. */
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val hasSearched: Boolean = false,
    val results: List<EmailSummary> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val errorMessage: String? = null,
) {
    val hasActiveFilters: Boolean
        get() = from.isNotBlank() || to.isNotBlank() || subject.isNotBlank() ||
            dateStart.isNotBlank() || dateEnd.isNotBlank() ||
            isRead != null || isStarred != null || hasAttachment != null
}

class SearchViewModel(
    private val searchEmails: SearchEmailsUseCase,
    private val setEmailStarred: SetEmailStarredUseCase,
    private val mailboxId: String,
) : ViewModel() {

    companion object {
        /** Long enough to skip a request per keystroke, short enough to feel live. */
        const val DEBOUNCE_MILLIS = 350L
        private const val PAGE_SIZE = 50
    }

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    /** The one in-flight (or debounce-pending) first-page search. Each new intent cancels the
     * previous job, so a stale response can never overwrite a newer query's results. */
    private var searchJob: Job? = null

    /** The query that produced the current [SearchUiState.results]. [loadMore] pages through
     * this — never the live field values, which the user may have edited since. */
    private var submittedQuery: SearchQuery? = null

    fun onQueryChanged(value: String) {
        _state.update { it.copy(queryText = value, errorMessage = null) }
        scheduleSearch(debounce = true)
    }

    fun onFromChanged(value: String) = _state.update { it.copy(from = value) }
    fun onToChanged(value: String) = _state.update { it.copy(to = value) }
    fun onSubjectChanged(value: String) = _state.update { it.copy(subject = value) }

    fun onDateStartChanged(value: String) = _state.update {
        it.copy(dateStart = value, dateError = validateDates(value, it.dateEnd))
    }

    fun onDateEndChanged(value: String) = _state.update {
        it.copy(dateEnd = value, dateError = validateDates(it.dateStart, value))
    }

    fun onReadFilterChanged(value: Boolean?) = _state.update { it.copy(isRead = value) }
    fun onStarredFilterChanged(value: Boolean?) = _state.update { it.copy(isStarred = value) }
    fun onAttachmentFilterChanged(value: Boolean?) = _state.update { it.copy(hasAttachment = value) }

    fun clearFilters() = _state.update {
        it.copy(
            from = "", to = "", subject = "", dateStart = "", dateEnd = "",
            isRead = null, isStarred = null, hasAttachment = null, dateError = null,
        )
    }

    fun consumeError() = _state.update { it.copy(errorMessage = null) }

    /** Explicit search (IME action, filter Apply): fires immediately, replacing any in-flight
     * or debounce-pending search rather than being swallowed by it. */
    fun search() = scheduleSearch(debounce = false)

    private fun scheduleSearch(debounce: Boolean) {
        searchJob?.cancel()
        val current = _state.value
        if (current.queryText.isBlank() && !current.hasActiveFilters) {
            // Nothing to search for isn't an error — it's a fresh page.
            submittedQuery = null
            _state.update {
                it.copy(results = emptyList(), totalCount = 0, page = 1, hasSearched = false, loading = false, loadingMore = false)
            }
            return
        }
        if (current.dateError != null) return
        searchJob = viewModelScope.launch {
            if (debounce) delay(DEBOUNCE_MILLIS)
            executeSearch()
        }
    }

    private suspend fun executeSearch() {
        val current = _state.value
        val query = current.toQuery()
        _state.update { it.copy(loading = true, errorMessage = null) }
        searchEmails(mailboxId, query, page = 1, limit = PAGE_SIZE)
            .onSuccess { pageResult ->
                submittedQuery = query
                _state.update {
                    it.copy(
                        loading = false,
                        hasSearched = true,
                        results = pageResult.emails,
                        totalCount = pageResult.totalCount,
                        page = 1,
                    )
                }
            }
            .onFailure { t ->
                // A repository that wraps exceptions in Result may hand us our own cancellation;
                // rethrowing keeps a cancelled search from reporting itself as a failure. On real
                // failures the existing results stay — an error must not blank the screen.
                if (t is CancellationException) throw t
                _state.update {
                    it.copy(loading = false, hasSearched = true, errorMessage = t.message ?: t::class.simpleName ?: "Search failed")
                }
            }
    }

    fun loadMore() {
        val query = submittedQuery ?: return
        val current = _state.value
        if (current.loading || current.loadingMore || current.results.size >= current.totalCount) return
        val nextPage = current.page + 1
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            searchEmails(mailboxId, query, page = nextPage, limit = PAGE_SIZE)
                .onSuccess { pageResult ->
                    _state.update {
                        it.copy(
                            loadingMore = false,
                            // distinctBy: if the backend shifted between pages, a repeated id
                            // would crash the list (duplicate LazyColumn keys).
                            results = (it.results + pageResult.emails).distinctBy { e -> e.id },
                            totalCount = pageResult.totalCount,
                            page = nextPage,
                        )
                    }
                }
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { it.copy(loadingMore = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to load more") }
                }
        }
    }

    fun toggleStarred(email: EmailSummary) {
        val newStarred = !email.starred
        _state.update { current ->
            current.copy(results = current.results.map { if (it.id == email.id) it.copy(starred = newStarred) else it })
        }
        viewModelScope.launch {
            setEmailStarred(mailboxId, email.id, newStarred)
                .onFailure { t ->
                    if (t is CancellationException) throw t
                    _state.update { current ->
                        current.copy(
                            results = current.results.map { if (it.id == email.id) it.copy(starred = email.starred) else it },
                            errorMessage = t.message ?: t::class.simpleName ?: "Failed to update star",
                        )
                    }
                }
        }
    }

    private fun validateDates(start: String, end: String): String? {
        fun parseable(value: String): Boolean = try {
            LocalDate.parse(value)
            true
        } catch (e: Exception) {
            false
        }

        val trimmedStart = start.trim()
        val trimmedEnd = end.trim()
        return when {
            trimmedStart.isNotEmpty() && !parseable(trimmedStart) -> "Dates must be YYYY-MM-DD"
            trimmedEnd.isNotEmpty() && !parseable(trimmedEnd) -> "Dates must be YYYY-MM-DD"
            trimmedStart.isNotEmpty() && trimmedEnd.isNotEmpty() &&
                LocalDate.parse(trimmedEnd) < LocalDate.parse(trimmedStart) -> "End date is before start date"
            else -> null
        }
    }
}

private fun SearchUiState.toQuery(): SearchQuery = SearchQuery(
    query = queryText.trim(),
    from = from.trim().ifBlank { null },
    to = to.trim().ifBlank { null },
    subject = subject.trim().ifBlank { null },
    dateStart = dateStart.trim().ifBlank { null },
    dateEnd = dateEnd.trim().ifBlank { null },
    isRead = isRead,
    isStarred = isStarred,
    hasAttachment = hasAttachment,
)
