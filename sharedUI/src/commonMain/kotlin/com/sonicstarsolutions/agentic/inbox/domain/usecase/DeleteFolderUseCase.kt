package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.repository.FolderRepository

class DeleteFolderUseCase(
    private val repository: FolderRepository,
) {
    suspend operator fun invoke(mailboxId: String, folderId: String): Result<Unit> =
        repository.deleteFolder(mailboxId, folderId)
}
