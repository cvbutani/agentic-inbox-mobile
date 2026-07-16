package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail

interface ThreadRepository {
    /** Fetches every message in [threadId]'s conversation; if the email has no thread (a
     * single, non-threaded message), pass threadId = null to fetch just [emailId] instead. */
    suspend fun getThread(mailboxId: String, emailId: String, threadId: String?): Result<List<EmailDetail>>
}
