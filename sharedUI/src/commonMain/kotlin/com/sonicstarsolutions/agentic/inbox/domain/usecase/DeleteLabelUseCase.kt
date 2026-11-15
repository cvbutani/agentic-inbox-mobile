package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.repository.LabelRepository

class DeleteLabelUseCase(private val repository: LabelRepository) {
    suspend operator fun invoke(mailboxId: String, labelId: String): Result<Unit> =
        repository.deleteLabel(mailboxId, labelId)
}
