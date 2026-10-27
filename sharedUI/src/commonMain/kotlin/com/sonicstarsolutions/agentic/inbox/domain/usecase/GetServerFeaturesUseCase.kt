package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.data.network.dto.FeatureFlagsDto
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository

class GetServerFeaturesUseCase(
    private val repository: MailboxRepository,
) {
    suspend operator fun invoke(): Result<FeatureFlagsDto> = repository.getServerFeatures()
}
