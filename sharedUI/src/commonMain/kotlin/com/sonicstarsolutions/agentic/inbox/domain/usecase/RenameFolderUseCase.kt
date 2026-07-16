package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.repository.FolderRepository

class RenameFolderUseCase(
    private val repository: FolderRepository,
) {
    suspend operator fun invoke(mailboxId: String, folderId: String, name: String): Result<Folder> =
        repository.renameFolder(mailboxId, folderId, name)
}
