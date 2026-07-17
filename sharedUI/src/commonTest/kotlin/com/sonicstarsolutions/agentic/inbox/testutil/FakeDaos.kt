package com.sonicstarsolutions.agentic.inbox.testutil

import com.sonicstarsolutions.agentic.inbox.data.local.EmailDao
import com.sonicstarsolutions.agentic.inbox.data.local.EmailEntity
import com.sonicstarsolutions.agentic.inbox.data.local.FolderDao
import com.sonicstarsolutions.agentic.inbox.data.local.FolderEntity
import com.sonicstarsolutions.agentic.inbox.data.local.MailboxDao
import com.sonicstarsolutions.agentic.inbox.data.local.MailboxEntity

/** Hand-written in-memory implementations of Room's `@Dao` interfaces — Room's own generated
 * implementation can't be exercised in a JVM host test (Android's builder needs a real
 * `Context`), so repository tests verify cache-fallback behavior against these instead. */
class FakeMailboxDao : MailboxDao {
    private val rows = mutableListOf<MailboxEntity>()

    override suspend fun upsertAll(mailboxes: List<MailboxEntity>) {
        mailboxes.forEach { new -> rows.removeAll { it.id == new.id }; rows.add(new) }
    }

    override suspend fun getAll(): List<MailboxEntity> = rows.toList()

    override suspend fun deleteAll() {
        rows.clear()
    }
}

class FakeFolderDao : FolderDao {
    private val rows = mutableListOf<FolderEntity>()

    override suspend fun upsertAll(folders: List<FolderEntity>) {
        folders.forEach { new -> rows.removeAll { it.mailboxId == new.mailboxId && it.id == new.id }; rows.add(new) }
    }

    override suspend fun getForMailbox(mailboxId: String): List<FolderEntity> = rows.filter { it.mailboxId == mailboxId }

    override suspend fun deleteForMailbox(mailboxId: String) {
        rows.removeAll { it.mailboxId == mailboxId }
    }

    override suspend fun deleteAll() {
        rows.clear()
    }
}

class FakeEmailDao : EmailDao {
    private val rows = mutableListOf<EmailEntity>()

    override suspend fun upsertAll(emails: List<EmailEntity>) {
        emails.forEach { new -> rows.removeAll { it.mailboxId == new.mailboxId && it.id == new.id }; rows.add(new) }
    }

    override suspend fun getForFolder(mailboxId: String, folderId: String): List<EmailEntity> =
        rows.filter { it.mailboxId == mailboxId && it.folderId == folderId }.sortedByDescending { it.date }

    override suspend fun deleteForFolder(mailboxId: String, folderId: String) {
        rows.removeAll { it.mailboxId == mailboxId && it.folderId == folderId }
    }

    override suspend fun deleteAll() {
        rows.clear()
    }
}
