package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.DomainSettings
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository

class GetDomainSettingsUseCase(
    private val repository: MailboxRepository,
) {
    suspend operator fun invoke(domain: String): Result<DomainSettings> = repository.getDomainSettings(domain)
}
