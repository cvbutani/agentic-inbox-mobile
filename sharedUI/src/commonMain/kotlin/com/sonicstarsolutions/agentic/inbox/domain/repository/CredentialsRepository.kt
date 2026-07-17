package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import kotlinx.coroutines.flow.StateFlow

interface CredentialsRepository {
    val state: StateFlow<Credentials>

    /**
     * Updates [state] only — does not persist. The HTTP client reads [state] to build every
     * outgoing request (base URL, auth headers), so credentials must be staged here before a
     * connection can even be attempted with them. Use this while a set of credentials is still
     * unconfirmed; call [save] only once they're known to work, so a failed connection attempt
     * can never leave bad credentials durably stored.
     */
    suspend fun stage(credentials: Credentials)

    /** Persists to disk and updates [state]. */
    suspend fun save(credentials: Credentials)
    suspend fun clear()
    suspend fun loadIntoState()
}
