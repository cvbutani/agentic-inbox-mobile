package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.dto.FolderDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.FolderNameDto
import com.sonicstarsolutions.agentic.inbox.data.network.safeApiCall
import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
import com.sonicstarsolutions.agentic.inbox.domain.repository.FolderRepository

class FolderRepositoryImpl(
    private val api: AgenticInboxApi,
) : FolderRepository {
    override suspend fun getFolders(mailboxId: String): Result<List<Folder>> =
        safeApiCall {
            val byId: Map<String, FolderDto> = api.getFolders(mailboxId).associateBy { it.id }

            // The Worker isn't guaranteed to include system folders in this response (they may be
            // implicit), so defaults always win on identity/order — only the unread count merges in.
            val systemFolders = SystemFolders.defaults.map { default ->
                byId[default.id]?.let { dto -> default.copy(unreadCount = dto.unreadCount) } ?: default
            }
            val customFolders = byId.values
                .filter { it.id !in SystemFolders.ids }
                .map { dto -> Folder(id = dto.id, name = dto.name, unreadCount = dto.unreadCount, isSystem = false) }

            systemFolders + customFolders
        }

    override suspend fun createFolder(mailboxId: String, name: String): Result<Folder> =
        safeApiCall {
            val dto = api.createFolder(mailboxId, FolderNameDto(name))
            Folder(id = dto.id, name = dto.name, unreadCount = dto.unreadCount, isSystem = false)
        }
}
