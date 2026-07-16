package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository

class GetEmailsUseCase(
    private val repository: EmailRepository,
) {
    suspend operator fun invoke(
        mailboxId: String,
        folder: String,
        page: Int,
        limit: Int,
    ): Result<EmailPage> = repository.getEmails(mailboxId, folder, page, limit)
}
