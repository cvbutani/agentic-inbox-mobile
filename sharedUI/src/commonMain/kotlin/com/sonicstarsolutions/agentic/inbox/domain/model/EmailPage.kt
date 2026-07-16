package com.sonicstarsolutions.agentic.inbox.domain.model

data class EmailPage(
    val emails: List<EmailSummary>,
    val totalCount: Int,
)
