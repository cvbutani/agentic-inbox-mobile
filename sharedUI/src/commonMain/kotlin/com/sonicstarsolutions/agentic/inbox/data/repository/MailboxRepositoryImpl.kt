package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.dto.MailboxDto
import com.sonicstarsolutions.agentic.inbox.data.network.safeApiCall
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository

class MailboxRepositoryImpl(
    private val api: AgenticInboxApi,
) : MailboxRepository {
    override suspend fun getMailboxes(): Result<List<Mailbox>> =
        safeApiCall { api.listMailboxes().map { it.toDomain() } }
}

private fun MailboxDto.toDomain(): Mailbox = Mailbox(id = id, email = email, name = name)
