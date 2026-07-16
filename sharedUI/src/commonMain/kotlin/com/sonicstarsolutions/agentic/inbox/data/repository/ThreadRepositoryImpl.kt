package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.dto.AttachmentDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.EmailFullDto
import com.sonicstarsolutions.agentic.inbox.data.network.safeApiCall
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailAttachment
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import com.sonicstarsolutions.agentic.inbox.domain.repository.ThreadRepository

class ThreadRepositoryImpl(
    private val api: AgenticInboxApi,
) : ThreadRepository {
    override suspend fun getThread(mailboxId: String, emailId: String, threadId: String?): Result<List<EmailDetail>> =
        safeApiCall {
            if (threadId != null) {
                api.getThread(mailboxId, threadId).map { it.toDomain() }
            } else {
                listOf(api.getEmail(mailboxId, emailId).toDomain())
            }
        }
}

private fun EmailFullDto.toDomain(): EmailDetail = EmailDetail(
    id = id,
    subject = subject,
    sender = sender,
    recipient = recipient,
    cc = cc,
    bcc = bcc,
    date = date,
    read = read,
    starred = starred,
    threadId = threadId,
    folderId = folderId,
    body = body,
    attachments = attachments.map { it.toDomain() },
)

private fun AttachmentDto.toDomain(): EmailAttachment = EmailAttachment(
    id = id,
    filename = filename,
    mimetype = mimetype,
    size = size,
)
