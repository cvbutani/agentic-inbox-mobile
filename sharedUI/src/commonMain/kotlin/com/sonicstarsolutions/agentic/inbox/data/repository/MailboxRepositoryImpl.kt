package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.local.MailboxDao
import com.sonicstarsolutions.agentic.inbox.data.local.MailboxEntity
import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.dto.CreateMailboxDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.MailboxDto
import com.sonicstarsolutions.agentic.inbox.data.network.safeApiCall
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository

class MailboxRepositoryImpl(
    private val api: AgenticInboxApi,
    private val mailboxDao: MailboxDao,
) : MailboxRepository {
    override suspend fun getMailboxes(): Result<List<Mailbox>> {
        val networkResult = safeApiCall { api.listMailboxes().map { it.toDomain() } }
        networkResult.onSuccess { mailboxes ->
            mailboxDao.deleteAll()
            mailboxDao.upsertAll(mailboxes.map { it.toEntity() })
        }
        if (networkResult.isSuccess) return networkResult

        val cached = mailboxDao.getAll().map { it.toDomain() }
        return if (cached.isNotEmpty()) Result.success(cached) else networkResult
    }

    override suspend fun createMailbox(email: String, name: String): Result<Mailbox> =
        safeApiCall { api.createMailbox(CreateMailboxDto(email = email, name = name)).toDomain() }

    override suspend fun getAllowedDomains(): Result<List<String>> =
        safeApiCall { api.getConfig().domains }

    override suspend fun getMailbox(mailboxId: String): Result<Mailbox> =
        safeApiCall { api.getMailbox(mailboxId).toDomain() }

    override suspend fun deleteMailbox(mailboxId: String): Result<Unit> =
        safeApiCall { api.deleteMailbox(mailboxId) }

    override suspend fun clearCache() = mailboxDao.deleteAll()
}

private fun MailboxDto.toDomain(): Mailbox = Mailbox(id = id, email = email, name = name)

private fun Mailbox.toEntity(): MailboxEntity = MailboxEntity(id = id, email = email, name = name)

private fun MailboxEntity.toDomain(): Mailbox = Mailbox(id = id, email = email, name = name)
