package com.sonicstarsolutions.agentic.inbox.data.local

import androidx.room.Entity

@Entity(tableName = "folders", primaryKeys = ["mailboxId", "id"])
data class FolderEntity(
    val mailboxId: String,
    val id: String,
    val name: String,
    val unreadCount: Int,
    val isSystem: Boolean,
)
