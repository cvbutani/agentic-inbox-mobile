package com.sonicstarsolutions.agentic.inbox.ui.mailbox.agentaccess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sonicstarsolutions.agentic.inbox.domain.model.AgentKey
import com.sonicstarsolutions.agentic.inbox.domain.model.AgentScope
import com.sonicstarsolutions.agentic.inbox.domain.model.CreatedAgentKey
import com.sonicstarsolutions.agentic.inbox.domain.model.NewAgentKey
import com.sonicstarsolutions.agentic.inbox.domain.usecase.CreateAgentKeyUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.ListAgentKeysUseCase
import com.sonicstarsolutions.agentic.inbox.domain.usecase.RevokeAgentKeyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgentAccessUiState(
    val loading: Boolean = true,
    /** Set when the initial load failed — replaces the screen with an error + Retry. */
    val loadError: String? = null,
    /** Active keys first, then revoked ones; newest first within each group. */
    val keys: List<AgentKey> = emptyList(),
    /** A create or revoke is in flight. */
    val saving: Boolean = false,
    /** Set right after creating a key, and only until the user dismisses it: this is the single
     * time the secret exists on the device. */
    val createdKey: CreatedAgentKey? = null,
    /** One glanceable snackbar line. */
    val message: String? = null,
)

class AgentAccessViewModel(
    private val listKeys: ListAgentKeysUseCase,
    private val createKey: CreateAgentKeyUseCase,
    private val revokeKey: RevokeAgentKeyUseCase,
    private val mailboxId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(AgentAccessUiState())
    val state: StateFlow<AgentAccessUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() {
        _state.update { it.copy(loading = true, loadError = null) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            listKeys(mailboxId)
                .onSuccess { keys -> _state.update { it.copy(loading = false, keys = keys.ordered()) } }
                .onFailure { t ->
                    _state.update { it.copy(loading = false, loadError = t.message ?: "Couldn't load agent keys") }
                }
        }
    }

    fun create(name: String, scopes: Set<AgentScope>, dailySendLimit: Int?) {
        if (_state.value.saving) return
        val trimmed = name.trim()
        if (trimmed.isEmpty() || scopes.isEmpty()) {
            _state.update { it.copy(message = "Name the key and pick at least one permission") }
            return
        }
        val canSend = AgentScope.SEND in scopes
        if (canSend && dailySendLimit != null && dailySendLimit < 1) {
            _state.update { it.copy(message = "The daily send limit must be at least 1") }
            return
        }

        val newKey = NewAgentKey(
            name = trimmed,
            // enum order, not selection order, so what's sent doesn't depend on tap order.
            scopes = AgentScope.entries.filter { it in scopes },
            // A limit only means something to a key that can send.
            dailySendLimit = dailySendLimit.takeIf { canSend },
        )
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            createKey(mailboxId, newKey)
                .onSuccess { created ->
                    _state.update {
                        it.copy(saving = false, createdKey = created, keys = (it.keys + created.key).ordered())
                    }
                }
                .onFailure { t ->
                    _state.update { it.copy(saving = false, message = t.message ?: "Couldn't create the key") }
                }
        }
    }

    fun dismissSecret() {
        _state.update { it.copy(createdKey = null) }
    }

    fun revoke(keyId: String) {
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            revokeKey(mailboxId, keyId)
                .onSuccess {
                    _state.update { current ->
                        current.copy(
                            saving = false,
                            // Mirrors what the server did, so the row updates without a refetch.
                            keys = current.keys.map { if (it.id == keyId && !it.revoked) it.copy(revokedAt = REVOKED_LOCALLY) else it }.ordered(),
                            message = "Key revoked",
                        )
                    }
                }
                .onFailure { t ->
                    _state.update { it.copy(saving = false, message = t.message ?: "Couldn't revoke the key") }
                }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun List<AgentKey>.ordered(): List<AgentKey> =
        sortedWith(compareBy<AgentKey> { it.revoked }.thenByDescending { it.createdAt })

    private companion object {
        /** Placeholder until the next list load brings the server's real timestamp. */
        const val REVOKED_LOCALLY = "revoked"
    }
}
