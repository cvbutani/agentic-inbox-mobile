package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.Credentials
import kotlinx.coroutines.flow.StateFlow

interface CredentialsRepository {
    val state: StateFlow<Credentials>

    suspend fun save(credentials: Credentials)
    suspend fun clear()
    suspend fun loadIntoState()
}
