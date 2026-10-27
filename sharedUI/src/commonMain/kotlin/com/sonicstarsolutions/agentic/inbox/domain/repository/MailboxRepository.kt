package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.data.network.dto.FeatureFlagsDto
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.model.MailboxSettings

interface MailboxRepository {
    suspend fun getMailboxes(): Result<List<Mailbox>>

    suspend fun createMailbox(email: String, name: String): Result<Mailbox>

    /** Domains new mailbox addresses may be created under, per the Worker's own config. */
    suspend fun getAllowedDomains(): Result<List<String>>

    /** Feature flags reported by the Worker. Returns conservative defaults if the Worker
     * is an older version that does not expose a `features` object. */
    suspend fun getServerFeatures(): Result<FeatureFlagsDto>

    suspend fun getMailbox(mailboxId: String): Result<Mailbox>

    suspend fun getMailboxSettings(mailboxId: String): Result<MailboxSettings>

    /** Saves [settings] without disturbing any setting the app doesn't edit (the server replaces
     * the whole blob, so this reads, merges and writes back). Returns what the server stored. */
    suspend fun updateMailboxSettings(mailboxId: String, settings: MailboxSettings): Result<MailboxSettings>

    suspend fun deleteMailbox(mailboxId: String): Result<Unit>

    /** Wipes the locally cached mailbox list — the offline-read mirror, not anything server-side.
     * Used on sign-out so a different account signing in on this device never reads the previous
     * account's cache through [getMailboxes]'s offline fallback. */
    suspend fun clearCache()
}
