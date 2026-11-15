package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.dto.ScheduleRequestDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.ScheduledEmailDto
import com.sonicstarsolutions.agentic.inbox.data.network.safeApiCall
import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduleKind
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduledEmail
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduledStatus
import com.sonicstarsolutions.agentic.inbox.domain.repository.ScheduleRepository
import kotlin.time.Instant

class ScheduleRepositoryImpl(
    private val api: AgenticInboxApi,
) : ScheduleRepository {
    override suspend fun schedule(
        mailboxId: String,
        request: ComposeEmailRequest,
        kind: ScheduleKind,
        originalEmailId: String?,
        sendAt: Instant,
    ): Result<Unit> =
        safeApiCall {
            val body = ScheduleRequestDto(
                kind = if (kind == ScheduleKind.REPLY) "reply" else "compose",
                originalEmailId = originalEmailId.takeIf { kind == ScheduleKind.REPLY },
                sendAt = sendAt.toString(),
                email = request.toDto(),
            )
            api.scheduleEmail(mailboxId, body)
        }.map {}

    override suspend fun getScheduled(mailboxId: String): Result<List<ScheduledEmail>> =
        safeApiCall { api.getScheduled(mailboxId).scheduled.map { it.toDomain() } }

    override suspend fun cancel(mailboxId: String, scheduledId: String): Result<Unit> =
        safeApiCall { api.cancelScheduled(mailboxId, scheduledId) }
}

private fun ScheduledEmailDto.toDomain() = ScheduledEmail(
    id = id,
    kind = if (kind == "reply") ScheduleKind.REPLY else ScheduleKind.COMPOSE,
    sendAt = sendAt,
    // A status a newer server adds shows as waiting rather than vanishing from the list.
    status = when (status) {
        "sending" -> ScheduledStatus.SENDING
        "failed" -> ScheduledStatus.FAILED
        else -> ScheduledStatus.PENDING
    },
    error = error,
    to = to,
    subject = subject,
)
