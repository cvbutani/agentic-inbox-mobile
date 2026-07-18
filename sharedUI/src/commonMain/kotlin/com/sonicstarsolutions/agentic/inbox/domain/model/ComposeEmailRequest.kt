package com.sonicstarsolutions.agentic.inbox.domain.model

data class ComposeEmailRequest(
    val fromEmail: String,
    val fromName: String,
    val to: List<String>,
    val cc: List<String> = emptyList(),
    val bcc: List<String> = emptyList(),
    val subject: String,
    val body: String,
    val attachments: List<ComposeAttachment> = emptyList(),
)

/** A file the user attached in the composer, carried as raw bytes; the data layer base64-encodes
 * it into the Worker's send schema. */
class ComposeAttachment(
    val filename: String,
    val mimeType: String,
    val bytes: ByteArray,
)
