package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.model.SearchQuery
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository

class SearchEmailsUseCase(
    private val repository: EmailRepository,
) {
    suspend operator fun invoke(
        mailboxId: String,
        query: SearchQuery,
        page: Int,
        limit: Int,
    ): Result<EmailPage> = repository.search(mailboxId, query, page, limit)
}
