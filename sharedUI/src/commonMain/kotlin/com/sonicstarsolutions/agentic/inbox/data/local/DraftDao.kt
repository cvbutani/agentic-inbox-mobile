package com.sonicstarsolutions.agentic.inbox.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Upsert
    suspend fun upsert(draft: DraftEntity)

    @Query("SELECT * FROM drafts WHERE id = :id")
    suspend fun getById(id: String): DraftEntity?

    /** Observed rather than fetched: the Drafts folder updates live while the composer autosaves. */
    @Query("SELECT * FROM drafts WHERE mailboxId = :mailboxId ORDER BY updatedAt DESC")
    fun observeForMailbox(mailboxId: String): Flow<List<DraftEntity>>

    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun deleteById(id: String)
}
