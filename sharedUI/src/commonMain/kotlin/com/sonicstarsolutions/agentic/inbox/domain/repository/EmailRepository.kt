package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage

interface EmailRepository {
    suspend fun getEmails(
        mailboxId: String,
        folder: String,
        page: Int,
        limit: Int,
    ): Result<EmailPage>

    suspend fun moveEmail(mailboxId: String, emailId: String, folderId: String): Result<Unit>
    suspend fun deleteEmail(mailboxId: String, emailId: String): Result<Unit>
    suspend fun setRead(mailboxId: String, emailId: String, read: Boolean): Result<Unit>
    suspend fun markThreadRead(mailboxId: String, threadId: String): Result<Unit>
}
