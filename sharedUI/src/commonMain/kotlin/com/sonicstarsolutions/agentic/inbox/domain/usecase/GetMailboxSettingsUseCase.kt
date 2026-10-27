package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.MailboxSettings
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository

class GetMailboxSettingsUseCase(
    private val repository: MailboxRepository,
) {
    suspend operator fun invoke(mailboxId: String): Result<MailboxSettings> = repository.getMailboxSettings(mailboxId)
}
