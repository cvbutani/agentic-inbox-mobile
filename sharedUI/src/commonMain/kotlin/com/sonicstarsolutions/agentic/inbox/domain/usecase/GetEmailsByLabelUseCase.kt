package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository

class GetEmailsByLabelUseCase(private val repository: EmailRepository) {
    suspend operator fun invoke(mailboxId: String, labelId: String, page: Int, limit: Int): Result<EmailPage> =
        repository.getEmailsByLabel(mailboxId, labelId, page, limit)
}
