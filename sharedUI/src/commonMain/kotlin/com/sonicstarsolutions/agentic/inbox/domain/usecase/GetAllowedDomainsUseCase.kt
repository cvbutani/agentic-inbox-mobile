package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository

class GetAllowedDomainsUseCase(
    private val repository: MailboxRepository,
) {
    suspend operator fun invoke(): Result<List<String>> = repository.getAllowedDomains()
}
