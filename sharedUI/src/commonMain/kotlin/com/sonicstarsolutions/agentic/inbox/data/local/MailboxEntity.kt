package com.sonicstarsolutions.agentic.inbox.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mailboxes")
data class MailboxEntity(
    @PrimaryKey val id: String,
    val email: String,
    val name: String,
    val fromName: String? = null,
)
