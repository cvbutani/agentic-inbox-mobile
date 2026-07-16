package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.repository.FolderRepository

class CreateFolderUseCase(
    private val repository: FolderRepository,
) {
    suspend operator fun invoke(mailboxId: String, name: String): Result<Folder> =
        repository.createFolder(mailboxId, name)
}
