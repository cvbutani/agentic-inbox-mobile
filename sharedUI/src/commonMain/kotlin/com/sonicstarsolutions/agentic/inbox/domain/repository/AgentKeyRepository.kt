package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.AgentKey
import com.sonicstarsolutions.agentic.inbox.domain.model.CreatedAgentKey
import com.sonicstarsolutions.agentic.inbox.domain.model.NewAgentKey

/**
 * Scoped agent access keys for one mailbox. Each call fails with
 * [com.sonicstarsolutions.agentic.inbox.domain.model.FeatureUnavailableException] when the Worker
 * doesn't have agent access turned on (or predates it).
 */
interface AgentKeyRepository {
    suspend fun listKeys(mailboxId: String): Result<List<AgentKey>>

    suspend fun createKey(mailboxId: String, newKey: NewAgentKey): Result<CreatedAgentKey>

    suspend fun revokeKey(mailboxId: String, keyId: String): Result<Unit>
}
