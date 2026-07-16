package com.sonicstarsolutions.agentic.inbox.domain.model

data class SearchQuery(
    val query: String,
    val folder: String? = null,
    val from: String? = null,
    val to: String? = null,
    val subject: String? = null,
    val dateStart: String? = null,
    val dateEnd: String? = null,
    val isRead: Boolean? = null,
    val isStarred: Boolean? = null,
    val hasAttachment: Boolean? = null,
)
