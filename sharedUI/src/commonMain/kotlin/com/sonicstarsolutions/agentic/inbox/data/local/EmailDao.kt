package com.sonicstarsolutions.agentic.inbox.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface EmailDao {
    @Upsert
    suspend fun upsertAll(emails: List<EmailEntity>)

    @Query("SELECT * FROM emails WHERE mailboxId = :mailboxId AND folderId = :folderId ORDER BY date DESC")
    suspend fun getForFolder(mailboxId: String, folderId: String): List<EmailEntity>

    @Query("DELETE FROM emails WHERE mailboxId = :mailboxId AND folderId = :folderId")
    suspend fun deleteForFolder(mailboxId: String, folderId: String)
}
