package com.sonicstarsolutions.agentic.inbox.domain.repository

/**
 * Remembers, per domain, which of its mailboxes a message was last sent from, so composing from
 * the domain-wide "All inboxes" starts from the mailbox the user most likely wants. Device-local.
 */
interface LastSenderStore {
    suspend fun lastMailbox(domain: String): String?
    suspend fun remember(domain: String, mailboxId: String)
}
