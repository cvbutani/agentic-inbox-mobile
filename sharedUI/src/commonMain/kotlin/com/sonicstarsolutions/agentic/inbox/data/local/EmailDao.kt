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

    /** Everything cached for a domain's mailboxes in one folder, newest first — the offline
     * "All inboxes". Mailbox ids are addresses, so the domain is whatever follows the @. */
    @Query("SELECT * FROM emails WHERE mailboxId LIKE '%@' || :domain AND folderId = :folderId ORDER BY date DESC LIMIT :limit")
    suspend fun getForDomainFolder(domain: String, folderId: String, limit: Int): List<EmailEntity>

    @Query("DELETE FROM emails WHERE mailboxId = :mailboxId AND folderId = :folderId")
    suspend fun deleteForFolder(mailboxId: String, folderId: String)

    @Query("DELETE FROM emails")
    suspend fun deleteAll()
}
