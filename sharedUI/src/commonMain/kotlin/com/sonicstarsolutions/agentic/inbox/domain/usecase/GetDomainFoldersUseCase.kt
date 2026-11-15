package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.repository.FolderRepository

class GetDomainFoldersUseCase(
    private val repository: FolderRepository,
) {
    suspend operator fun invoke(domain: String): Result<List<Folder>> = repository.getDomainFolders(domain)
}
