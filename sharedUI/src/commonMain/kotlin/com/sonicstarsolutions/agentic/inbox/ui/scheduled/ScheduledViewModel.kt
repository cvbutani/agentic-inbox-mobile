package com.sonicstarsolutions.agentic.inbox.ui.scheduled

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduledEmail
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CancelScheduledEmailUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetScheduledEmailsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduledUiState(
    val loading: Boolean = true,
    /** Set when the first load failed — replaces the screen with an error and Retry. */
    val loadError: String? = null,
    val items: List<ScheduledEmail> = emptyList(),
    /** One glanceable snackbar line: a cancel's result or a refresh failure. */
    val message: String? = null,
)

/** The messages waiting on the server to be sent later, with the ability to cancel each. */
class ScheduledViewModel(
    private val getScheduled: GetScheduledEmailsUseCase,
    private val cancelScheduled: CancelScheduledEmailUseCase,
    private val mailboxId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduledUiState())
    val state: StateFlow<ScheduledUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() {
        _state.update { it.copy(loading = true, loadError = null) }
        load()
    }

    /** Reloads without blanking the list, so a pull-to-refresh or a returning screen doesn't flash. */
    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            getScheduled(mailboxId)
                .onSuccess { list -> _state.update { it.copy(loading = false, loadError = null, items = list) } }
                .onFailure { t ->
                    val text = t.message ?: t::class.simpleName ?: "Couldn't load scheduled messages"
                    // With rows already on screen a failed refresh is only worth a snackbar.
                    _state.update { if (it.items.isEmpty()) it.copy(loading = false, loadError = text) else it.copy(loading = false, message = text) }
                }
        }
    }

    fun cancel(item: ScheduledEmail) {
        viewModelScope.launch {
            cancelScheduled(mailboxId, item.id)
                .onSuccess {
                    _state.update { it.copy(items = it.items.filterNot { row -> row.id == item.id }, message = "Scheduled message cancelled") }
                }
                .onFailure { t ->
                    _state.update { it.copy(message = t.message ?: t::class.simpleName ?: "Couldn't cancel") }
                    // Usually because it just started sending — show where it really stands.
                    load()
                }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}
