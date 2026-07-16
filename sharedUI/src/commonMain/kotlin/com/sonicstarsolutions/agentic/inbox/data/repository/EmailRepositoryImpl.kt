package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.dto.EmailMetadataDto
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
)
