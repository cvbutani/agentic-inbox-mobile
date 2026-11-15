package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Contact
import com.sonicstarsolutions.agentic.inbox.domain.repository.ContactRepository

class SearchContactsUseCase(private val repository: ContactRepository) {
    suspend operator fun invoke(mailboxId: String, query: String, limit: Int): Result<List<Contact>> =
        repository.search(mailboxId, query, limit)
}
