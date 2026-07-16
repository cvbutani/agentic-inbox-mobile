package com.sonicstarsolutions.agentic.inbox.domain.model

data class EmailSummary(
    val id: String,
    val subject: String,
    val sender: String,
    val recipient: String,
    val date: String,
    val read: Boolean,
    val starred: Boolean,
    val threadId: String?,
    val folderId: String?,
    val snippet: String?,
)
