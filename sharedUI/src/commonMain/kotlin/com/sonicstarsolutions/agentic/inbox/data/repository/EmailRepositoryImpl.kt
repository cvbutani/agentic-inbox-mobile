package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.dto.EmailMetadataDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.MoveEmailRequestDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.UpdateEmailDto
import com.sonicstarsolutions.agentic.inbox.data.network.safeApiCall
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository

class EmailRepositoryImpl(
    private val api: AgenticInboxApi,
) : EmailRepository {
    override suspend fun getEmails(
        mailboxId: String,
        folder: String,
        page: Int,
        limit: Int,
    ): Result<EmailPage> =
        safeApiCall {
            val pageDto = api.getEmails(mailboxId, folder = folder, page = page, limit = limit)
            EmailPage(pageDto.emails.map { it.toDomain() }, pageDto.totalCount)
        }

    override suspend fun moveEmail(mailboxId: String, emailId: String, folderId: String): Result<Unit> =
        safeApiCall { api.moveEmail(mailboxId, emailId, MoveEmailRequestDto(folderId)) }.map {}

    override suspend fun deleteEmail(mailboxId: String, emailId: String): Result<Unit> =
        safeApiCall { api.deleteEmail(mailboxId, emailId) }

    override suspend fun setRead(mailboxId: String, emailId: String, read: Boolean): Result<Unit> =
        safeApiCall { api.updateEmail(mailboxId, emailId, UpdateEmailDto(read = read)) }.map {}

    override suspend fun markThreadRead(mailboxId: String, threadId: String): Result<Unit> =
        safeApiCall { api.markThreadRead(mailboxId, threadId) }.map {}
}

private fun EmailMetadataDto.toDomain(): EmailSummary = EmailSummary(
    id = id,
    subject = subject,
    sender = sender,
    recipient = recipient,
    date = date,
    read = read,
    starred = starred,
    threadId = threadId,
    folderId = folderId,
    snippet = snippet,
    threadUnreadCount = threadUnreadCount,
)
