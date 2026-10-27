package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.CreatedAgentKey
import com.sonicstarsolutions.agentic.inbox.domain.model.NewAgentKey
import com.sonicstarsolutions.agentic.inbox.domain.repository.AgentKeyRepository

class CreateAgentKeyUseCase(
    private val repository: AgentKeyRepository,
) {
    suspend operator fun invoke(mailboxId: String, newKey: NewAgentKey): Result<CreatedAgentKey> =
        repository.createKey(mailboxId, newKey)
}
