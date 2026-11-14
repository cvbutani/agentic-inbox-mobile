package com.sonicstarsolutions.agentic.inbox.domain.model

/** A change applied to many emails in one request. */
enum class BulkAction { READ, UNREAD, STAR, UNSTAR, MOVE, DELETE }

/**
 * Which of the requested emails a bulk update changed. The two sets partition the requested ids:
 * a request is sent in chunks, so one chunk can fail while the others succeed.
 */
data class BulkOutcome(val succeeded: Set<String>, val failed: Set<String>)
