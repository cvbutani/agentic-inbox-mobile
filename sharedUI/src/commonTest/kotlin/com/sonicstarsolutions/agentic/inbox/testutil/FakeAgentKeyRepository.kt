package com.sonicstarsolutions.agentic.inbox.testutil

import com.sonicstarsolutions.agentic.inbox.domain.model.AgentKey
import com.sonicstarsolutions.agentic.inbox.domain.model.AgentScope
import com.sonicstarsolutions.agentic.inbox.domain.model.CreatedAgentKey
import com.sonicstarsolutions.agentic.inbox.domain.model.NewAgentKey
import com.sonicstarsolutions.agentic.inbox.domain.repository.AgentKeyRepository
import kotlinx.coroutines.CompletableDeferred

/** In-memory keys that behave like the Worker: creating adds an active key, revoking marks one. */
class FakeAgentKeyRepository(initial: List<AgentKey> = emptyList()) : AgentKeyRepository {
    val keys = initial.toMutableList()

    var listFailure: Throwable? = null
    var listCalls = 0

    var createFailure: Throwable? = null
    val createCalls = mutableListOf<NewAgentKey>()

    /** Holds [createKey] in flight, to exercise the double-tap guard. */
    var createGate: CompletableDeferred<Unit>? = null

    var revokeFailure: Throwable? = null
    val revokeCalls = mutableListOf<String>()

    override suspend fun listKeys(mailboxId: String): Result<List<AgentKey>> {
        listCalls++
        return listFailure?.let { Result.failure(it) } ?: Result.success(keys.toList())
    }

    override suspend fun createKey(mailboxId: String, newKey: NewAgentKey): Result<CreatedAgentKey> {
        createCalls += newKey
        createGate?.await()
        createFailure?.let { return Result.failure(it) }
        val key = agentKey(id = "new${keys.size}", name = newKey.name, scopes = newKey.scopes, dailySendLimit = newKey.dailySendLimit)
        keys += key
        return Result.success(CreatedAgentKey(key, secret = "agt_${key.id}_SECRET"))
    }

    override suspend fun revokeKey(mailboxId: String, keyId: String): Result<Unit> {
        revokeCalls += keyId
        revokeFailure?.let { return Result.failure(it) }
        val index = keys.indexOfFirst { it.id == keyId }
        if (index >= 0) keys[index] = keys[index].copy(revokedAt = "2026-10-27T10:00:00.000Z")
        return Result.success(Unit)
    }
}

fun agentKey(
    id: String = "k1",
    name: String = "Key $id",
    scopes: List<AgentScope> = listOf(AgentScope.READ),
    dailySendLimit: Int? = null,
    createdAt: String = "2026-10-27T09:00:00.000Z",
    revokedAt: String? = null,
) = AgentKey(
    id = id,
    prefix = "agt_$id",
    name = name,
    scopes = scopes,
    dailySendLimit = dailySendLimit,
    createdAt = createdAt,
    revokedAt = revokedAt,
)
