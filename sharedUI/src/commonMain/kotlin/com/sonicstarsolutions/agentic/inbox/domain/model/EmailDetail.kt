package com.sonicstarsolutions.agentic.inbox.domain.model

data class EmailDetail(
    val id: String,
    val subject: String,
    val sender: String,
    val recipient: String,
    val cc: String?,
    val bcc: String?,
    val date: String,
    val read: Boolean,
    val starred: Boolean,
    val threadId: String?,
    val folderId: String?,
    val body: String?,
    val attachments: List<EmailAttachment>,
    /**
     * The id of the message this one replies to. Present on a draft that was started as a reply —
     * the server stores it on the draft row itself and reading it back is how the app later knows
     * to send that draft via the reply endpoint (targeting this id) rather than as a new email.
     * Null for a message that isn't a reply (or a from-scratch draft).
     */
    val inReplyTo: String? = null,
)

data class EmailAttachment(
    val id: String,
    val filename: String,
    val mimetype: String,
    val size: Long,
)
