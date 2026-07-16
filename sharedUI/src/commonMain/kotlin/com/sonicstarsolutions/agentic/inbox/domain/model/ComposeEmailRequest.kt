package com.sonicstarsolutions.agentic.inbox.domain.model

data class ComposeEmailRequest(
    val fromEmail: String,
    val fromName: String,
    val to: List<String>,
    val cc: List<String> = emptyList(),
    val bcc: List<String> = emptyList(),
    val subject: String,
    val body: String,
)
