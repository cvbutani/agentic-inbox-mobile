package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailAttachment

interface AttachmentRepository {
    /** Cache-first: returns the absolute path of the attachment's bytes on local disk,
     * downloading them only when no complete cached copy exists. */
    suspend fun download(mailboxId: String, emailId: String, attachment: EmailAttachment): Result<String>
}
