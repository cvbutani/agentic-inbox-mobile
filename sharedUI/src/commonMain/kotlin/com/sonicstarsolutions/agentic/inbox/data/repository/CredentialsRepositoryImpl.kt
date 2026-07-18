package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.local.CredentialsStorage
import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import com.sonicstarsolutions.agentic.inbox.domain.repository.CredentialsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CredentialsRepositoryImpl(
    private val storage: CredentialsStorage,
    scope: CoroutineScope,
) : CredentialsRepository {

    private val _state = MutableStateFlow(Credentials())
    override val state: StateFlow<Credentials> = _state.asStateFlow()

    init {
        // Self-hydration, not trust in Splash: an Activity recreation (theme change, locale,
        // process-death restore) rebuilds the DI graph while the saveable nav backstack skips
        // Splash — the only caller of loadIntoState(). Without this, every request after such a
        // recreation fired with blank credentials. compareAndSet so a slow disk read can only
        // fill an *empty* state, never clobber something staged or saved in the meantime.
        scope.launch {
            val loaded = storage.read()
            _state.compareAndSet(Credentials(), loaded)
        }
    }

    override suspend fun stage(credentials: Credentials) {
        _state.update { credentials }
    }

    override suspend fun save(credentials: Credentials) {
        storage.write(credentials)
        _state.update { credentials }
    }

    override suspend fun clear() {
        storage.delete()
        _state.update { Credentials() }
    }

    override suspend fun loadIntoState() {
        _state.value = storage.read()
    }
}
