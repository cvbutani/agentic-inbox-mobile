package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.DomainSettings
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.model.MailboxSettings

interface MailboxRepository {
    suspend fun getMailboxes(): Result<List<Mailbox>>

    suspend fun createMailbox(email: String, name: String): Result<Mailbox>

    /** Domains new mailbox addresses may be created under, per the Worker's own config. */
    suspend fun getAllowedDomains(): Result<List<String>>

    suspend fun getMailbox(mailboxId: String): Result<Mailbox>

    suspend fun getMailboxSettings(mailboxId: String): Result<MailboxSettings>

    /** Saves [settings] without disturbing any setting the app doesn't edit (the server replaces
     * the whole blob, so this reads, merges and writes back). Returns what the server stored. */
    suspend fun updateMailboxSettings(mailboxId: String, settings: MailboxSettings): Result<MailboxSettings>

    /** The defaults every mailbox on [domain] inherits (currently the auto-delete retention). */
    suspend fun getDomainSettings(domain: String): Result<DomainSettings>

    /** Sets how many days mail on [domain] is kept; mailboxes with their own retention are unaffected. */
    suspend fun updateDomainSettings(domain: String, retentionDays: Int): Result<DomainSettings>

    suspend fun deleteMailbox(mailboxId: String): Result<Unit>

    /** Wipes the locally cached mailbox list — the offline-read mirror, not anything server-side.
     * Used on sign-out so a different account signing in on this device never reads the previous
     * account's cache through [getMailboxes]'s offline fallback. */
    suspend fun clearCache()
}
