package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.dto.CreateMailboxDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.MailboxDto
import com.sonicstarsolutions.agentic.inbox.data.network.safeApiCall
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository

class MailboxRepositoryImpl(
    private val api: AgenticInboxApi,
) : MailboxRepository {
    override suspend fun getMailboxes(): Result<List<Mailbox>> =
        safeApiCall { api.listMailboxes().map { it.toDomain() } }

    override suspend fun createMailbox(email: String, name: String): Result<Mailbox> =
        safeApiCall { api.createMailbox(CreateMailboxDto(email = email, name = name)).toDomain() }

    override suspend fun getAllowedDomains(): Result<List<String>> =
        safeApiCall { api.getConfig().domains }

    override suspend fun getMailbox(mailboxId: String): Result<Mailbox> =
        safeApiCall { api.getMailbox(mailboxId).toDomain() }
}

private fun MailboxDto.toDomain(): Mailbox = Mailbox(id = id, email = email, name = name)
