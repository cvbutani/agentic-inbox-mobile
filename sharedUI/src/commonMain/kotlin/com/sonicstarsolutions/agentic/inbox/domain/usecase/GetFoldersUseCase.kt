package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.repository.FolderRepository

class GetFoldersUseCase(
    private val repository: FolderRepository,
) {
    suspend operator fun invoke(mailboxId: String): Result<List<Folder>> = repository.getFolders(mailboxId)
}
