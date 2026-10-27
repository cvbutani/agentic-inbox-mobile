package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.repository.AgentKeyRepository

class RevokeAgentKeyUseCase(
    private val repository: AgentKeyRepository,
) {
    suspend operator fun invoke(mailboxId: String, keyId: String): Result<Unit> = repository.revokeKey(mailboxId, keyId)
}
