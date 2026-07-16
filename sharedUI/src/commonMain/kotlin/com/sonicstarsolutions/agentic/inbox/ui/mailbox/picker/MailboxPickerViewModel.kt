package com.sonicstarsolutions.agentic.inbox.ui.mailbox.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ClearCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MailboxPickerUiState(
    val loading: Boolean = true,
    val mailboxes: List<Mailbox> = emptyList(),
    val errorMessage: String? = null,
    val signedOut: Boolean = false,
)

class MailboxPickerViewModel(
    private val getMailboxes: GetMailboxesUseCase,
    private val clearCredentials: ClearCredentialsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MailboxPickerUiState())
    val state: StateFlow<MailboxPickerUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        try {
            _state.update { it.copy(loading = true, errorMessage = null) }
            viewModelScope.launch(Dispatchers.IO) {
                getMailboxes()
                    .onSuccess { list ->
                        _state.update {
                            it.copy(
                                loading = false,
                                mailboxes = list
                            )
                        }
                    }
                    .onFailure { t ->
                        _state.update {
                            it.copy(
                                loading = false,
                                errorMessage = t.message ?: t::class.simpleName ?: "Failed to load"
                            )
                        }
                    }
            }
        } catch (ex: Exception) {
            print(ex)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            clearCredentials()
            _state.update { it.copy(signedOut = true) }
        }
    }

    fun consumeSignedOut() = _state.update { it.copy(signedOut = false) }
}
