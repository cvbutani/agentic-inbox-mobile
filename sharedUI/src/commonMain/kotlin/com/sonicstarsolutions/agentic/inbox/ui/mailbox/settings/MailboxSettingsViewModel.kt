package com.sonicstarsolutions.agentic.inbox.ui.mailbox.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.DEFAULT_RETENTION_DAYS
import com.sonicstarsolutions.agentic.inbox.domain.model.MailboxSettings
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetDomainSettingsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.GetMailboxSettingsUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.UpdateDomainSettingsUseCase
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
    /** Why the last alias entered was refused; cleared by the next accepted alias. */
    val aliasError: String? = null,
    val saving: Boolean = false,
    /** One glanceable snackbar line — a save result, success or failure. */
    val message: String? = null,
    /** The mailbox's domain, blank when the id has none (nothing to inherit from then). */
    val domain: String = "",
    /** The domain's default retention in days, once known; null until loaded or when it couldn't be. */
    val domainRetentionDays: Int? = null,
    val savingDomain: Boolean = false,
) {
    val dirty: Boolean get() = form != saved

    /** Days this mailbox's mail is really kept: its own setting, else its domain's, else the
     * server's default. 0 is forever. */
    val effectiveRetentionDays: Int get() = form.retentionDays ?: domainRetentionDays ?: DEFAULT_RETENTION_DAYS
}

class MailboxSettingsViewModel(
    private val getSettings: GetMailboxSettingsUseCase,
    private val updateSettings: UpdateMailboxSettingsUseCase,
    private val getDomainSettings: GetDomainSettingsUseCase,
    private val updateDomainSettings: UpdateDomainSettingsUseCase,
    private val mailboxId: String,
) : ViewModel() {

    private val domain = mailboxId.substringAfter('@', missingDelimiterValue = "").lowercase()

    private val _state = MutableStateFlow(MailboxSettingsUiState(domain = domain))
    val state: StateFlow<MailboxSettingsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() {
        _state.update { it.copy(loading = true, loadError = null) }
        load()
    }

    private fun load() {
        loadDomainDefault()
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

    /** Non-fatal: without it the screen just can't say what "use the domain default" resolves to. */
    private fun loadDomainDefault() {
        if (domain.isBlank()) return
        viewModelScope.launch {
            getDomainSettings(domain).onSuccess { settings ->
                _state.update { it.copy(domainRetentionDays = settings.retentionDays) }
            }
        }
    }

    /** This mailbox's own retention in days (0 keeps everything); null follows the domain default. */
    fun onRetentionChanged(days: Int?) = edit { it.copy(retentionDays = days) }

    /** Sets the default for every mailbox on the domain. Saved at once, not with the mailbox form:
     * it is a different setting, owned by the domain, and it reaches mailboxes besides this one. */
    fun onDomainRetentionChosen(days: Int) {
        if (domain.isBlank() || _state.value.savingDomain) return
        _state.update { it.copy(savingDomain = true) }
        viewModelScope.launch {
            updateDomainSettings(domain, days)
                .onSuccess { stored ->
                    _state.update {
                        it.copy(savingDomain = false, domainRetentionDays = stored.retentionDays, message = "Default for $domain saved")
                    }
                }
                .onFailure { t ->
                    _state.update { it.copy(savingDomain = false, message = t.message ?: "Couldn't save the domain default") }
                }
        }
    }

    fun onFromNameChanged(value: String) = edit { it.copy(fromName = value) }
    fun onSignatureEnabledChanged(value: Boolean) = edit { it.copy(signatureEnabled = value) }
    fun onSignatureTextChanged(value: String) = edit { it.copy(signatureText = value) }

    fun onAliasRemoved(alias: String) = edit { it.copy(aliases = it.aliases.filterNot { a -> a == alias }) }

    /** Adds [raw] to the aliases after the checks the server would make anyway, so the user
     * learns why on the spot rather than after saving. Domain rules stay with the server. */
    fun onAliasAdded(raw: String) {
        val alias = raw.trim().lowercase()
        val current = _state.value.form.aliases
        val problem = when {
            !looksLikeEmail(alias) -> "“${raw.trim()}” isn't a valid email address."
            alias == mailboxId.lowercase() -> "An alias can't be the mailbox's own address."
            alias in current -> "$alias is already an alias."
            current.size >= MAX_ALIASES -> "A mailbox can have at most $MAX_ALIASES aliases."
            else -> null
        }
        if (problem != null) {
            _state.update { it.copy(aliasError = problem) }
            return
        }
        _state.update { it.copy(form = it.form.copy(aliases = current + alias), aliasError = null) }
    }

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

/** What the server accepts, in spirit: something@domain.tld with no spaces. */
private fun looksLikeEmail(value: String): Boolean {
    val at = value.indexOf('@')
    if (at <= 0 || at != value.lastIndexOf('@') || value.any { it.isWhitespace() }) return false
    val domain = value.substring(at + 1)
    return '.' in domain && !domain.startsWith('.') && !domain.endsWith('.')
}

private const val MAX_ALIASES = 20
