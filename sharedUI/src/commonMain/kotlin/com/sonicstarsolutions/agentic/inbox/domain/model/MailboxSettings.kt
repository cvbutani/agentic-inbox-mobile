package com.sonicstarsolutions.agentic.inbox.domain.model

/**
 * The per-mailbox preferences the app lets the user edit. Everything else the Worker keeps in a
 * mailbox's settings (auto-reply, aliases, auto-draft, agent prompt, …) is deliberately absent: saving
 * preserves it untouched rather than modelling what the app can't edit.
 *
 * The signature is plain text only; rich HTML signatures are a web-client feature.
 */
data class MailboxSettings(
    val fromName: String = "",
    val signatureEnabled: Boolean = false,
    val signatureText: String = "",
    /** Extra addresses the mailbox receives mail at and can send from, lowercase. */
    val aliases: List<String> = emptyList(),
)
