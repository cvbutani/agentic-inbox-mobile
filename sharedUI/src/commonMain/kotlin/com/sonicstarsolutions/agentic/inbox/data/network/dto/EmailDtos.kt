package com.sonicstarsolutions.agentic.inbox.data.network.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmailMetadataDto(
    val id: String,
    val subject: String,
    val sender: String,
    val recipient: String,
    val cc: String? = null,
    val bcc: String? = null,
    val date: String,
    val read: Boolean = false,
    val starred: Boolean = false,
    @SerialName("in_reply_to") val inReplyTo: String? = null,
    @SerialName("email_references") val emailReferences: String? = null,
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("folder_id") val folderId: String? = null,
    val snippet: String? = null,
    @SerialName("thread_unread_count") val threadUnreadCount: Int = 0,
    /** Messages in the conversation (this folder only) — 1 for a lone message or a non-threaded list. */
    @SerialName("thread_count") val threadCount: Int = 1,
    @SerialName("labels") val labels: List<LabelDto> = emptyList(),
)

@Serializable
data class EmailPageDto(
    val emails: List<EmailMetadataDto> = emptyList(),
    val totalCount: Int = 0,
)

@Serializable
data class EmailFullDto(
    val id: String,
    val subject: String,
    val sender: String,
    val recipient: String,
    val cc: String? = null,
    val bcc: String? = null,
    val date: String,
    val read: Boolean = false,
    val starred: Boolean = false,
    @SerialName("in_reply_to") val inReplyTo: String? = null,
    @SerialName("email_references") val emailReferences: String? = null,
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("folder_id") val folderId: String? = null,
    val snippet: String? = null,
    val body: String? = null,
    @SerialName("message_id") val messageId: String? = null,
    @SerialName("raw_headers") val rawHeaders: String? = null,
    val attachments: List<AttachmentDto> = emptyList(),
)

@Serializable
data class AttachmentDto(
    @SerialName("id") val id: String,
    @SerialName("filename") val filename: String,
    @SerialName("mimetype") val mimetype: String,
    @SerialName("size") val size: Long,
    @SerialName("content_id") val contentId: String? = null,
    @SerialName("disposition") val disposition: String? = null,
)

/**
 * Body shared by send/reply/forward — the Worker validates all three against the same
 * `SendEmailRequestSchema` (workers/lib/schemas.ts).
 */
@Serializable
data class SendEmailRequestDto(
    @SerialName("to") val to: List<String>,
    @SerialName("cc") val cc: List<String>? = null,
    @SerialName("bcc") val bcc: List<String>? = null,
    @SerialName("from") val from: FromDto,
    @SerialName("subject") val subject: String,
    @SerialName("html") val html: String? = null,
    @SerialName("text") val text: String? = null,
    @SerialName("attachments") val attachments: List<OutboundAttachmentDto>? = null,
    @SerialName("in_reply_to") val inReplyTo: String? = null,
    @SerialName("references") val references: List<String>? = null,
    @SerialName("thread_id") val threadId: String? = null,
)

@Serializable
data class FromDto(
    @SerialName("email") val email: String,
    @SerialName("name") val name: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class OutboundAttachmentDto(
    @SerialName("content") val content: String, // base64 encoded
    @SerialName("filename") val filename: String,
    @SerialName("type") val type: String, // MIME type
    // The server's schema requires this field, so it must be sent even when it equals the default
    // (the client's Json has encodeDefaults = false).
    @EncodeDefault @SerialName("disposition") val disposition: String = "attachment", // "attachment" | "inline"
    @SerialName("contentId") val contentId: String? = null,
)

@Serializable
data class SendEmailResponseDto(
    @SerialName("id") val id: String,
    @SerialName("status") val status: String,
)

@Serializable
data class UpdateEmailDto(
    @SerialName("read") val read: Boolean? = null,
    @SerialName("starred") val starred: Boolean? = null,
)

@Serializable
data class MoveEmailRequestDto(
    @SerialName("folderId") val folderId: String,
)

@Serializable
data class StatusResponseDto(
    @SerialName("status") val status: String,
)

/** Body of `POST …/emails/bulk`. The server accepts at most 200 ids per request. */
@Serializable
data class BulkRequestDto(
    @SerialName("action") val action: String,
    @SerialName("ids") val ids: List<String>,
    /** Conversations to act on as a whole (every message in [folder]); needs [folder]. */
    @SerialName("thread_ids") val threadIds: List<String>? = null,
    /** The folder the list is showing — thread targets only affect messages in it. */
    @SerialName("folder") val folder: String? = null,
    @SerialName("folderId") val folderId: String? = null,
    @SerialName("labelId") val labelId: String? = null,
)

@Serializable
data class BulkResponseDto(
    @SerialName("count") val count: Int = 0,
)

/** Body of `POST …/emails/schedule`: the whole email plus when to send it. */
@Serializable
data class ScheduleRequestDto(
    @SerialName("kind") val kind: String,
    @SerialName("originalEmailId") val originalEmailId: String? = null,
    @SerialName("send_at") val sendAt: String,
    @SerialName("email") val email: SendEmailRequestDto,
)

@Serializable
data class ScheduleResponseDto(
    @SerialName("id") val id: String = "",
    @SerialName("status") val status: String = "",
    @SerialName("send_at") val sendAt: String = "",
)

@Serializable
data class ScheduledEmailDto(
    @SerialName("id") val id: String,
    @SerialName("kind") val kind: String = "compose",
    @SerialName("send_at") val sendAt: String,
    @SerialName("status") val status: String = "pending",
    @SerialName("error") val error: String? = null,
    @SerialName("to") val to: String = "",
    @SerialName("subject") val subject: String = "",
)

@Serializable
data class ScheduledListDto(
    @SerialName("scheduled") val scheduled: List<ScheduledEmailDto> = emptyList(),
)
