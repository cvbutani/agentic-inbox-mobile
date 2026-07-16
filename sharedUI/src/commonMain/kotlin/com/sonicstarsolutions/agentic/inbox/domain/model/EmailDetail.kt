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
)

data class EmailAttachment(
    val id: String,
    val filename: String,
    val mimetype: String,
    val size: Long,
)
