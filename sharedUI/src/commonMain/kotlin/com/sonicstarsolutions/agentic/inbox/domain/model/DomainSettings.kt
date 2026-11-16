package com.sonicstarsolutions.agentic.inbox.domain.model

/**
 * Defaults a whole domain hands to the mailboxes on it. [retentionDays] is how long mail is kept
 * before the server's daily cleanup deletes it; 0 keeps mail forever. A mailbox that sets its own
 * retention overrides this.
 */
data class DomainSettings(
    val domain: String,
    val retentionDays: Int,
)

/** Retention the server applies when neither the mailbox nor its domain says anything: never delete. */
const val DEFAULT_RETENTION_DAYS = 0
