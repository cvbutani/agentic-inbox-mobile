package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.model.SearchQuery
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository

class SearchDomainEmailsUseCase(
    private val repository: EmailRepository,
) {
    suspend operator fun invoke(domain: String, query: SearchQuery, cursor: String?, limit: Int): Result<EmailPage> =
        repository.searchDomain(domain, query, cursor, limit)
}
