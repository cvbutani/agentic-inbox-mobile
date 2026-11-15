package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.Label
import com.sonicstarsolutions.agentic.inbox.domain.repository.LabelRepository

class GetLabelsUseCase(private val repository: LabelRepository) {
    suspend operator fun invoke(mailboxId: String): Result<List<Label>> = repository.getLabels(mailboxId)
}
