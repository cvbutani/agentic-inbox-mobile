package com.sonicstarsolutions.agentic.inbox.data.local

import androidx.room.ColumnInfo
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
    /** The email's labels as a JSON array of LabelDto; null when it has none. */
    val labelsJson: String? = null,
    /** Messages in the conversation the row stands for; 1 for a lone message. */
    @ColumnInfo(defaultValue = "1")
    val threadCount: Int = 1,
)
