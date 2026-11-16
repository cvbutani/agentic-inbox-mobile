package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository

class GetDomainEmailsUseCase(
    private val repository: EmailRepository,
) {
    suspend operator fun invoke(domain: String, folder: String, cursor: String?, limit: Int): Result<EmailPage> =
        repository.getDomainEmails(domain, folder, cursor, limit)
}
