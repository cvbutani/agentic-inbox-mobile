package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduleKind
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduledEmail
import com.sonicstarsolutions.agentic.inbox.domain.repository.ScheduleRepository
import kotlin.time.Instant

class CancelScheduledEmailUseCase(private val repository: ScheduleRepository) {
    suspend operator fun invoke(mailboxId: String, scheduledId: String): Result<Unit> = repository.cancel(mailboxId, scheduledId)
}
