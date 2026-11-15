package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.Label

interface LabelRepository {
    suspend fun getLabels(mailboxId: String): Result<List<Label>>

    suspend fun createLabel(mailboxId: String, name: String, color: String): Result<Label>

    /** Null fields are left unchanged on the server. */
    suspend fun updateLabel(mailboxId: String, labelId: String, name: String?, color: String?): Result<Label>

    suspend fun deleteLabel(mailboxId: String, labelId: String): Result<Unit>
}
