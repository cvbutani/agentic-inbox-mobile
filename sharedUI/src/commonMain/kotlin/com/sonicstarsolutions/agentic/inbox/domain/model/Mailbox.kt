package com.sonicstarsolutions.agentic.inbox.domain.model

data class Mailbox(
    val id: String,
    val email: String,
    val name: String,
    /** The mailbox owner's configured display name (server-side `settings.fromName`) — only
     * present on the single-mailbox GET, never the list endpoint. Null until fetched at least
     * once; see [displayName] for the fallback callers should actually render. */
    val fromName: String? = null,
)

/** What to show for this mailbox everywhere a human-facing name is needed — the configured
 * [Mailbox.fromName] when there is one, otherwise [Mailbox.name]. */
val Mailbox.displayName: String
    get() = fromName?.takeIf { it.isNotBlank() } ?: name
