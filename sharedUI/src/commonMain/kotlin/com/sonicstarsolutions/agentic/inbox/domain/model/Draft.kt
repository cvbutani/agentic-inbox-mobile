package com.sonicstarsolutions.agentic.inbox.domain.model

/**
 * An unsent message the composer is holding onto.
 *
 * Drafts are **local only**. The Worker's API (see AgenticInboxApi) exposes no endpoint for
 * writing an email into the `draft` folder — sending is the only write path it offers — so a draft
 * lives in Room on this device and never syncs. The Drafts folder shows these alongside whatever
 * the server reports for that folder (e.g. drafts written by the web client), so nothing is hidden
 * either way.
 *
 * [mode] is the name of a [com.sonicstarsolutions.agentic.inbox.ui.compose.ComposeMode] value,
 * carried as a String for the same reason the Compose nav destination does it: the domain layer
 * shouldn't depend on a UI enum. Together with [originalEmailId] and [threadId] it's what lets a
 * resumed reply still reply to the right message.
 */
data class Draft(
    val id: String,
    val mailboxId: String,
    val to: String = "",
    val cc: String = "",
    val bcc: String = "",
    val subject: String = "",
    val body: String = "",
    val mode: String,
    val originalEmailId: String? = null,
    val threadId: String? = null,
    val updatedAt: Long,
) {
    /** A draft with nothing in it isn't worth keeping — the composer deletes rather than saves
     * these, so closing an untouched composer doesn't litter the Drafts folder. */
    val isEmpty: Boolean
        get() = to.isBlank() && cc.isBlank() && bcc.isBlank() && subject.isBlank() && body.isBlank()
}
