package com.sonicstarsolutions.agentic.inbox.ui.mailbox.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.MailboxSettings
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxSettingsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.UpdateMailboxSettingsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MailboxSettingsUiState(
    val loading: Boolean = true,
    /** Set when the initial load failed — replaces the whole screen with an error + Retry. */
    val loadError: String? = null,
    /** What the fields currently show. */
    val form: MailboxSettings = MailboxSettings(),
    /** What the server last had; [dirty] compares the form against it. */
    val saved: MailboxSettings = MailboxSettings(),
    val saving: Boolean = false,
    /** One glanceable snackbar line — a save result, success or failure. */
    val message: String? = null,
) {
    val dirty: Boolean get() = form != saved
}

class MailboxSettingsViewModel(
    private val getSettings: GetMailboxSettingsUseCase,
    private val updateSettings: UpdateMailboxSettingsUseCase,
    private val mailboxId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(MailboxSettingsUiState())
    val state: StateFlow<MailboxSettingsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() {
        _state.update { it.copy(loading = true, loadError = null) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            getSettings(mailboxId)
                .onSuccess { settings ->
                    _state.update { it.copy(loading = false, form = settings, saved = settings) }
                }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, loadError = t.message ?: "Couldn't load settings") }
                }
        }
    }

    fun onFromNameChanged(value: String) = edit { it.copy(fromName = value) }
    fun onSignatureEnabledChanged(value: Boolean) = edit { it.copy(signatureEnabled = value) }
    fun onSignatureTextChanged(value: String) = edit { it.copy(signatureText = value) }

    private fun edit(change: (MailboxSettings) -> MailboxSettings) {
        _state.update { it.copy(form = change(it.form)) }
    }

    fun save() {
        val current = _state.value
        if (current.loading || current.saving || !current.dirty) return
        val toSave = current.form.copy(fromName = current.form.fromName.trim())
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            updateSettings(mailboxId, toSave)
                .onSuccess { stored ->
                    _state.update { it.copy(saving = false, form = stored, saved = stored, message = "Settings saved") }
                }
                .onFailure { t ->
                    // Edits stay in the form, still dirty, so the user can simply try again.
                    _state.update { it.copy(saving = false, message = t.message ?: "Couldn't save settings") }
                }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}
