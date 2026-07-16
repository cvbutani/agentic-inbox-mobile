package com.sonicstarsolutions.agentic.inbox.domain.repository

interface ConnectionRepository {
    /** Verifies the currently-saved credentials can reach the configured Worker. */
    suspend fun validate(): Result<Unit>
}
