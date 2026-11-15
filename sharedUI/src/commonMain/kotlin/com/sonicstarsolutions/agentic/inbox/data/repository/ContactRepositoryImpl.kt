package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.safeApiCall
import com.sonicstarsolutions.agentic.inbox.domain.model.Contact
import com.sonicstarsolutions.agentic.inbox.domain.repository.ContactRepository

class ContactRepositoryImpl(
    private val api: AgenticInboxApi,
) : ContactRepository {
    override suspend fun search(mailboxId: String, query: String, limit: Int): Result<List<Contact>> =
        safeApiCall { api.getContacts(mailboxId, query = query, limit = limit).contacts.map { Contact(it.address, it.count) } }
}
