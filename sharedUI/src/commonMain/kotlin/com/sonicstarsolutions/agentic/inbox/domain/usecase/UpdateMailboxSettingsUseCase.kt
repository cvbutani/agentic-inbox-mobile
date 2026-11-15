package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.MailboxSettings
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository

class UpdateMailboxSettingsUseCase(
    private val repository: MailboxRepository,
) {
    suspend operator fun invoke(mailboxId: String, settings: MailboxSettings): Result<MailboxSettings> =
        repository.updateMailboxSettings(mailboxId, settings)
}
