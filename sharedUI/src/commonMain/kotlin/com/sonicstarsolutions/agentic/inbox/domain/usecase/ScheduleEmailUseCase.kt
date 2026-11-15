package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduleKind
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduledEmail
import com.sonicstarsolutions.agentic.inbox.domain.repository.ScheduleRepository
import kotlin.time.Instant

class ScheduleEmailUseCase(private val repository: ScheduleRepository) {
    suspend operator fun invoke(mailboxId: String, request: ComposeEmailRequest, kind: ScheduleKind, originalEmailId: String?, sendAt: Instant): Result<Unit> = repository.schedule(mailboxId, request, kind, originalEmailId, sendAt)
}
