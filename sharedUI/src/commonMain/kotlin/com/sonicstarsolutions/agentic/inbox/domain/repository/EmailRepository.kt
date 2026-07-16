package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.model.SearchQuery

interface EmailRepository {
    suspend fun getEmails(
        mailboxId: String,
        folder: String,
        page: Int,
        limit: Int,
    ): Result<EmailPage>

    suspend fun search(
        mailboxId: String,
        query: SearchQuery,
        page: Int,
        limit: Int,
    ): Result<EmailPage>

    suspend fun moveEmail(mailboxId: String, emailId: String, folderId: String): Result<Unit>
    suspend fun deleteEmail(mailboxId: String, emailId: String): Result<Unit>
    suspend fun setRead(mailboxId: String, emailId: String, read: Boolean): Result<Unit>
    suspend fun setStarred(mailboxId: String, emailId: String, starred: Boolean): Result<Unit>
    suspend fun markThreadRead(mailboxId: String, threadId: String): Result<Unit>

    suspend fun sendEmail(mailboxId: String, request: ComposeEmailRequest): Result<Unit>
    suspend fun replyEmail(mailboxId: String, emailId: String, request: ComposeEmailRequest): Result<Unit>
    suspend fun forwardEmail(mailboxId: String, emailId: String, request: ComposeEmailRequest): Result<Unit>
}
