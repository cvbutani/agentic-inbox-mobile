package com.sonicstarsolutions.agentic.inbox.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Unlike [EmailEntity], this table is not a cache — it's the only copy of the data (see
 * [com.sonicstarsolutions.agentic.inbox.domain.model.Draft]). It must never be dropped by a
 * destructive migration.
 */
@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val id: String,
    val mailboxId: String,
    val to: String,
    val cc: String,
    val bcc: String,
    val subject: String,
    val body: String,
    val mode: String,
    val originalEmailId: String?,
    val threadId: String?,
    val updatedAt: Long,
)
