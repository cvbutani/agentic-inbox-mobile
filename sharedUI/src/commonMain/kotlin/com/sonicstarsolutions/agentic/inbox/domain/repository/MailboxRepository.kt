package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox

interface MailboxRepository {
    suspend fun getMailboxes(): Result<List<Mailbox>>
}
