package com.sonicstarsolutions.agentic.inbox.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface FolderDao {
    @Upsert
    suspend fun upsertAll(folders: List<FolderEntity>)

    @Query("SELECT * FROM folders WHERE mailboxId = :mailboxId")
    suspend fun getForMailbox(mailboxId: String): List<FolderEntity>

    @Query("DELETE FROM folders WHERE mailboxId = :mailboxId")
    suspend fun deleteForMailbox(mailboxId: String)
}
