package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox

interface MailboxRepository {
    suspend fun getMailboxes(): Result<List<Mailbox>>

    suspend fun createMailbox(email: String, name: String): Result<Mailbox>

    /** Domains new mailbox addresses may be created under, per the Worker's own config. */
    suspend fun getAllowedDomains(): Result<List<String>>

    suspend fun getMailbox(mailboxId: String): Result<Mailbox>
}
