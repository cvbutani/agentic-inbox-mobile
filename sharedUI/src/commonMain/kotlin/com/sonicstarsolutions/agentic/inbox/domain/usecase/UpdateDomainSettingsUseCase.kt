package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.DomainSettings
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository

class UpdateDomainSettingsUseCase(
    private val repository: MailboxRepository,
) {
    suspend operator fun invoke(domain: String, retentionDays: Int): Result<DomainSettings> =
        repository.updateDomainSettings(domain, retentionDays)
}
