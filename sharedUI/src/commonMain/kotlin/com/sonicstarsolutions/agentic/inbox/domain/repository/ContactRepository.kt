package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.Contact

interface ContactRepository {
    /** Correspondents whose address contains [query], best match first. */
    suspend fun search(mailboxId: String, query: String, limit: Int): Result<List<Contact>>
}
