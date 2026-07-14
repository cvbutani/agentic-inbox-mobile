package com.sonicstarsolutions.agentic.inbox.ui.mailbox.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.dto.MailboxDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MailboxPickerUiState(
    val loading: Boolean = true,
    val mailboxes: List<MailboxDto> = emptyList(),
    val errorMessage: String? = null,
)

class MailboxPickerViewModel(
    private val api: AgenticInboxApi,
) : ViewModel() {

    private val _state = MutableStateFlow(MailboxPickerUiState())
    val state: StateFlow<MailboxPickerUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        _state.update { it.copy(loading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { api.listMailboxes() }
                .onSuccess { list -> _state.update { it.copy(loading = false, mailboxes = list) } }
                .onFailure { t ->
                    _state.update {
                        it.copy(loading = false, errorMessage = t.message ?: t::class.simpleName ?: "Failed to load")
                    }
                }
        }
    }
}
