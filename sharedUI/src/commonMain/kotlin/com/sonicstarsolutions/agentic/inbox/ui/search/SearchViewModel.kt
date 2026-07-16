package com.sonicstarsolutions.agentic.inbox.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.domain.model.SearchQuery
import com.sonicstarsolutions.agentic.inbox.domain.usecase.SearchEmailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val loading: Boolean = false,
    val hasSearched: Boolean = false,
    val results: List<EmailSummary> = emptyList(),
    val totalCount: Int = 0,
    val page: Int = 1,
    val errorMessage: String? = null,
)

class SearchViewModel(
    private val searchEmails: SearchEmailsUseCase,
    private val mailboxId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    fun onQueryChanged(value: String) = _state.update { it.copy(queryText = value, errorMessage = null) }
    fun onFromChanged(value: String) = _state.update { it.copy(from = value) }
    fun onToChanged(value: String) = _state.update { it.copy(to = value) }
    fun onSubjectChanged(value: String) = _state.update { it.copy(subject = value) }
    fun onDateStartChanged(value: String) = _state.update { it.copy(dateStart = value) }
    fun onDateEndChanged(value: String) = _state.update { it.copy(dateEnd = value) }
    fun onReadFilterChanged(value: Boolean?) = _state.update { it.copy(isRead = value) }
    fun onStarredFilterChanged(value: Boolean?) = _state.update { it.copy(isStarred = value) }
    fun onAttachmentFilterChanged(value: Boolean?) = _state.update { it.copy(hasAttachment = value) }

    fun clearFilters() = _state.update {
        it.copy(from = "", to = "", subject = "", dateStart = "", dateEnd = "", isRead = null, isStarred = null, hasAttachment = null)
    }

    fun consumeError() = _state.update { it.copy(errorMessage = null) }

    fun search() {
        val current = _state.value
        val trimmedQuery = current.queryText.trim()
        if (trimmedQuery.isBlank() && !current.hasAnyFilter()) {
            _state.update { it.copy(errorMessage = "Enter a search term or a filter.") }
            return
        }
        if (current.loading) return
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            searchEmails(mailboxId, current.toQuery(trimmedQuery), page = 1, limit = 50)
                .onSuccess { pageResult ->
                    _state.update {
                        it.copy(loading = false, hasSearched = true, results = pageResult.emails, totalCount = pageResult.totalCount, page = 1)
                    }
                }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, hasSearched = true, errorMessage = t.message ?: t::class.simpleName ?: "Search failed") }
                }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.results.size >= current.totalCount) return
        val nextPage = current.page + 1
        val trimmedQuery = current.queryText.trim()
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            searchEmails(mailboxId, current.toQuery(trimmedQuery), page = nextPage, limit = 50)
                .onSuccess { pageResult ->
                    _state.update {
                        it.copy(loading = false, results = it.results + pageResult.emails, totalCount = pageResult.totalCount, page = nextPage)
                    }
                }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to load more") }
                }
        }
    }
}

private fun SearchUiState.hasAnyFilter(): Boolean =
    from.isNotBlank() || to.isNotBlank() || subject.isNotBlank() || dateStart.isNotBlank() || dateEnd.isNotBlank() ||
        isRead != null || isStarred != null || hasAttachment != null

private fun SearchUiState.toQuery(trimmedQuery: String): SearchQuery = SearchQuery(
    query = trimmedQuery,
    from = from.trim().ifBlank { null },
    to = to.trim().ifBlank { null },
    subject = subject.trim().ifBlank { null },
    dateStart = dateStart.trim().ifBlank { null },
    dateEnd = dateEnd.trim().ifBlank { null },
    isRead = isRead,
    isStarred = isStarred,
    hasAttachment = hasAttachment,
)
