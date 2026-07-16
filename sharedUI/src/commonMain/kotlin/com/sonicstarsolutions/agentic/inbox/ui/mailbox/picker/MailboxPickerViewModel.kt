package com.sonicstarsolutions.agentic.inbox.ui.mailbox.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ClearCredentialsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CreateMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.DeleteMailboxUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetAllowedDomainsUseCase
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
    val allowedDomains: List<String> = emptyList(),
    val creatingMailbox: Boolean = false,
    val createMailboxError: String? = null,
    val mailboxCreated: Boolean = false,
    val deletingMailbox: Boolean = false,
    val deleteMailboxError: String? = null,
    val mailboxDeleted: Boolean = false,
)

class MailboxPickerViewModel(
    private val getMailboxes: GetMailboxesUseCase,
    private val clearCredentials: ClearCredentialsUseCase,
    private val createMailboxUseCase: CreateMailboxUseCase,
    private val getAllowedDomains: GetAllowedDomainsUseCase,
    private val deleteMailboxUseCase: DeleteMailboxUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MailboxPickerUiState())
    val state: StateFlow<MailboxPickerUiState> = _state.asStateFlow()

    init {
        refresh()
        loadAllowedDomains()
    }

    private fun loadAllowedDomains() {
        viewModelScope.launch {
            // Failure is non-fatal — the create-mailbox dialog just won't have domains to offer.
            getAllowedDomains().onSuccess { domains -> _state.update { it.copy(allowedDomains = domains) } }
        }
    }

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

    fun createMailbox(email: String, name: String) {
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()
        if (trimmedEmail.isBlank() || trimmedName.isBlank()) {
            _state.update { it.copy(createMailboxError = "Name and email are required.") }
            return
        }
        if (_state.value.creatingMailbox) return
        _state.update { it.copy(creatingMailbox = true, createMailboxError = null) }
        viewModelScope.launch {
            createMailboxUseCase(trimmedEmail, trimmedName)
                .onSuccess { mailbox ->
                    _state.update { it.copy(creatingMailbox = false, mailboxes = it.mailboxes + mailbox, mailboxCreated = true) }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(creatingMailbox = false, createMailboxError = t.message ?: t::class.simpleName ?: "Failed to create mailbox")
                    }
                }
        }
    }

    fun consumeMailboxCreated() = _state.update { it.copy(mailboxCreated = false) }

    fun consumeCreateMailboxError() = _state.update { it.copy(createMailboxError = null) }

    fun deleteMailbox(mailboxId: String) {
        if (_state.value.deletingMailbox) return
        _state.update { it.copy(deletingMailbox = true, deleteMailboxError = null) }
        viewModelScope.launch {
            deleteMailboxUseCase(mailboxId)
                .onSuccess {
                    _state.update {
                        it.copy(deletingMailbox = false, mailboxes = it.mailboxes.filterNot { m -> m.id == mailboxId }, mailboxDeleted = true)
                    }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(deletingMailbox = false, deleteMailboxError = t.message ?: t::class.simpleName ?: "Failed to delete mailbox")
                    }
                }
        }
    }

    fun consumeMailboxDeleted() = _state.update { it.copy(mailboxDeleted = false) }

    fun consumeDeleteMailboxError() = _state.update { it.copy(deleteMailboxError = null) }
}
