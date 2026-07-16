package com.sonicstarsolutions.agentic.inbox.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MailboxDao {
    @Upsert
    suspend fun upsertAll(mailboxes: List<MailboxEntity>)

    @Query("SELECT * FROM mailboxes")
    suspend fun getAll(): List<MailboxEntity>

    @Query("DELETE FROM mailboxes")
    suspend fun deleteAll()
}
