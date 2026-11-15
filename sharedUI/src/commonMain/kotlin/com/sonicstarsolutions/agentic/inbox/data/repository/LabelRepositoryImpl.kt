package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.dto.LabelDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.LabelInputDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.LabelUpdateDto
import com.sonicstarsolutions.agentic.inbox.data.network.safeApiCall
import com.sonicstarsolutions.agentic.inbox.domain.model.Label
import com.sonicstarsolutions.agentic.inbox.domain.repository.LabelRepository

class LabelRepositoryImpl(
    private val api: AgenticInboxApi,
) : LabelRepository {
    override suspend fun getLabels(mailboxId: String): Result<List<Label>> =
        safeApiCall { api.getLabels(mailboxId).labels.map { it.toDomain() } }

    override suspend fun createLabel(mailboxId: String, name: String, color: String): Result<Label> =
        safeApiCall { api.createLabel(mailboxId, LabelInputDto(name = name, color = color)).toDomain() }

    override suspend fun updateLabel(mailboxId: String, labelId: String, name: String?, color: String?): Result<Label> =
        safeApiCall { api.updateLabel(mailboxId, labelId, LabelUpdateDto(name = name, color = color)).toDomain() }

    override suspend fun deleteLabel(mailboxId: String, labelId: String): Result<Unit> =
        safeApiCall { api.deleteLabel(mailboxId, labelId) }
}

internal fun LabelDto.toDomain(): Label = Label(id = id, name = name, color = color, count = count)

internal fun Label.toDto(): LabelDto = LabelDto(id = id, name = name, color = color, count = count)
