package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.Folder

interface FolderRepository {
    /** System folders first (in SystemFolders.defaults order, with server-reported unread counts
     * merged in where present), followed by any custom folders the mailbox has created. */
    suspend fun getFolders(mailboxId: String): Result<List<Folder>>

    suspend fun createFolder(mailboxId: String, name: String): Result<Folder>

    suspend fun renameFolder(mailboxId: String, folderId: String, name: String): Result<Folder>

    suspend fun deleteFolder(mailboxId: String, folderId: String): Result<Unit>
}
