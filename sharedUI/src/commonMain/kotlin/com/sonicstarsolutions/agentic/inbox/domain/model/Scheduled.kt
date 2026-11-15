package com.sonicstarsolutions.agentic.inbox.domain.model

/** A scheduled message is either a fresh compose (new message, forward) or a reply, which the
 * server threads against the message it answers when the time comes. */
enum class ScheduleKind { COMPOSE, REPLY }

enum class ScheduledStatus { PENDING, SENDING, FAILED }

/** A message waiting on the server to be sent later. [sendAt] is the server's ISO timestamp. */
data class ScheduledEmail(
    val id: String,
    val kind: ScheduleKind,
    val sendAt: String,
    val status: ScheduledStatus,
    val error: String?,
    val to: String,
    val subject: String,
)
