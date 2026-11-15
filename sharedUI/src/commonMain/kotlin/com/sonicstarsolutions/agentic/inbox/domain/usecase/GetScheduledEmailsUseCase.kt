package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduleKind
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduledEmail
import com.sonicstarsolutions.agentic.inbox.domain.repository.ScheduleRepository
import kotlin.time.Instant

class GetScheduledEmailsUseCase(private val repository: ScheduleRepository) {
    suspend operator fun invoke(mailboxId: String): Result<List<ScheduledEmail>> = repository.getScheduled(mailboxId)
}
