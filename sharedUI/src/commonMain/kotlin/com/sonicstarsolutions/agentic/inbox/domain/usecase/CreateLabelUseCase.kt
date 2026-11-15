package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Label
import com.sonicstarsolutions.agentic.inbox.domain.repository.LabelRepository

class CreateLabelUseCase(private val repository: LabelRepository) {
    suspend operator fun invoke(mailboxId: String, name: String, color: String): Result<Label> =
        repository.createLabel(mailboxId, name, color)
}
