package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.AgentKey
import com.sonicstarsolutions.agentic.inbox.domain.repository.AgentKeyRepository

class ListAgentKeysUseCase(
    private val repository: AgentKeyRepository,
) {
    suspend operator fun invoke(mailboxId: String): Result<List<AgentKey>> = repository.listKeys(mailboxId)
}
