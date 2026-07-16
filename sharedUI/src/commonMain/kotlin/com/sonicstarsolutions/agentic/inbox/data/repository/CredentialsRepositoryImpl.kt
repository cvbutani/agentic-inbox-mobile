package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import com.sonicstarsolutions.agentic.inbox.domain.repository.CredentialsRepository
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CredentialsRepositoryImpl(
    private val ksafe: KSafe,
) : CredentialsRepository {
    companion object {
        private const val KEY_CREDENTIALS = "credentials"
    }

    // Starts empty; callers await loadIntoState() before relying on a hydrated value
    // (KSafe's read is suspend, so it can't run inside this property initializer).
    private val _state = MutableStateFlow(Credentials())
    override val state: StateFlow<Credentials> = _state.asStateFlow()

    override suspend fun save(credentials: Credentials) {
        ksafe.put(KEY_CREDENTIALS, credentials)
        _state.update { credentials }
    }

    override suspend fun clear() {
        ksafe.delete(KEY_CREDENTIALS)
        _state.update { Credentials() }
    }

    override suspend fun loadIntoState() {
        _state.value = ksafe.get(KEY_CREDENTIALS, Credentials())
    }
}
