package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.dto.AgentKeyDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.CreateAgentKeyDto
import com.sonicstarsolutions.agentic.inbox.data.network.isEndpointUnavailable
import com.sonicstarsolutions.agentic.inbox.data.network.safeApiCall
import com.sonicstarsolutions.agentic.inbox.domain.model.AgentKey
import com.sonicstarsolutions.agentic.inbox.domain.model.AgentScope
import com.sonicstarsolutions.agentic.inbox.domain.model.CreatedAgentKey
import com.sonicstarsolutions.agentic.inbox.domain.model.FeatureUnavailableException
import com.sonicstarsolutions.agentic.inbox.domain.model.NewAgentKey
import com.sonicstarsolutions.agentic.inbox.domain.repository.AgentKeyRepository

class AgentKeyRepositoryImpl(
    private val api: AgenticInboxApi,
) : AgentKeyRepository {

    override suspend fun listKeys(mailboxId: String): Result<List<AgentKey>> =
        safeApiCall { api.listAgentKeys(mailboxId).map { it.toDomain() } }.orUnavailable()

    override suspend fun createKey(mailboxId: String, newKey: NewAgentKey): Result<CreatedAgentKey> =
        safeApiCall {
            val dto = CreateAgentKeyDto(
                name = newKey.name,
                scopes = newKey.scopes.map { it.wireName },
                dailySendLimit = newKey.dailySendLimit,
            )
            api.createAgentKey(mailboxId, dto).let { CreatedAgentKey(it.key.toDomain(), it.secret) }
        }.orUnavailable()

    override suspend fun revokeKey(mailboxId: String, keyId: String): Result<Unit> =
        safeApiCall { api.revokeAgentKey(mailboxId, keyId) }.orUnavailable()
}

/** A 404/501 here means the Worker has agent access off or predates it — not a failed request. */
private fun <T> Result<T>.orUnavailable(): Result<T> = recoverCatching { t ->
    if (t.isEndpointUnavailable()) throw FeatureUnavailableException("Agent access isn't enabled on this server")
    throw t
}

private fun AgentKeyDto.toDomain() = AgentKey(
    id = id,
    prefix = prefix.ifEmpty { "agt_$id" },
    name = name,
    scopes = scopes.mapNotNull { AgentScope.fromWireName(it) },
    dailySendLimit = dailySendLimit,
    createdAt = createdAt,
    revokedAt = revokedAt,
)
