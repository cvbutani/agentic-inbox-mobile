package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduleKind
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduledEmail
import kotlin.time.Instant

interface ScheduleRepository {
    /** Queues [request] to go out at [sendAt]. [originalEmailId] is required for [ScheduleKind.REPLY]. */
    suspend fun schedule(
        mailboxId: String,
        request: ComposeEmailRequest,
        kind: ScheduleKind,
        originalEmailId: String?,
        sendAt: Instant,
    ): Result<Unit>

    suspend fun getScheduled(mailboxId: String): Result<List<ScheduledEmail>>

    /** Fails when the message is already being sent (or no longer exists). */
    suspend fun cancel(mailboxId: String, scheduledId: String): Result<Unit>
}
