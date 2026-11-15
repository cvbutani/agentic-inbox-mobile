package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Label
import com.sonicstarsolutions.agentic.inbox.domain.repository.LabelRepository

class UpdateLabelUseCase(private val repository: LabelRepository) {
    suspend operator fun invoke(mailboxId: String, labelId: String, name: String?, color: String?): Result<Label> =
        repository.updateLabel(mailboxId, labelId, name, color)
}
