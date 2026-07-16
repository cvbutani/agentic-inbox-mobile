package com.sonicstarsolutions.agentic.inbox.data.local

import androidx.room.Entity

@Entity(tableName = "emails", primaryKeys = ["mailboxId", "id"])
data class EmailEntity(
    val mailboxId: String,
    val id: String,
    val folderId: String,
    val subject: String,
    val sender: String,
    val recipient: String,
    val date: String,
    val read: Boolean,
    val starred: Boolean,
    val threadId: String?,
    val snippet: String?,
    val threadUnreadCount: Int,
)
