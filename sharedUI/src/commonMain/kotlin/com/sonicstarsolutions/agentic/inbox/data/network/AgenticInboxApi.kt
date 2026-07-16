package com.sonicstarsolutions.agentic.inbox.data.network

import com.sonicstarsolutions.agentic.inbox.data.network.dto.ConfigDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.CreateMailboxDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.EmailFullDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.EmailPageDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.FolderDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.FolderNameDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.MailboxDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.MoveEmailRequestDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.SendEmailRequestDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.SendEmailResponseDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.StatusResponseDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.UpdateEmailDto
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query

/**
 * Ktorfit-generated API client for the `cloudflare/agentic-inbox` Worker (workers/index.ts).
 * All endpoints are relative to the base URL configured in HttpClient.defaultRequest.
 *
 * Methods return plain (unwrapped) types and throw on failure — Ktorfit has no built-in Result<T>
 * converter (verified against ktorfit-lib-core 2.7.5: DefaultSuspendResponseConverterFactory just
 * rethrows on failure and tries to deserialize the body AS Result<T> on success, which isn't
 * valid). Callers in the data/repository implementations wrap calls in safeApiCall to produce the
 * Result<T> the domain layer expects.
 */
interface AgenticInboxApi {

    @GET("/api/v1/config")
    suspend fun getConfig(): ConfigDto

    @GET("/api/v1/mailboxes")
    suspend fun listMailboxes(): List<MailboxDto>

    @POST("/api/v1/mailboxes")
    suspend fun createMailbox(@Body body: CreateMailboxDto): MailboxDto

    @GET("/api/v1/mailboxes/{mailboxId}")
    suspend fun getMailbox(@Path("mailboxId") mailboxId: String): MailboxDto

    @DELETE("/api/v1/mailboxes/{mailboxId}")
    suspend fun deleteMailbox(@Path("mailboxId") mailboxId: String)

    // folder must be set for the response to include totalCount (see workers/index.ts) — an
    // omitted folder returns a bare array instead of {emails, totalCount} and won't parse as EmailPageDto.
    @GET("/api/v1/mailboxes/{mailboxId}/emails")
    suspend fun getEmails(
        @Path("mailboxId") mailboxId: String,
        @Query("folder") folder: String,
        @Query("thread_id") threadId: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("sortColumn") sortColumn: String? = null,
        @Query("sortDirection") sortDirection: String? = null,
    ): EmailPageDto

    @GET("/api/v1/mailboxes/{mailboxId}/emails/{emailId}")
    suspend fun getEmail(
        @Path("mailboxId") mailboxId: String,
        @Path("emailId") emailId: String,
    ): EmailFullDto

    @PUT("/api/v1/mailboxes/{mailboxId}/emails/{emailId}")
    suspend fun updateEmail(
        @Path("mailboxId") mailboxId: String,
        @Path("emailId") emailId: String,
        @Body body: UpdateEmailDto,
    ): EmailFullDto

    @DELETE("/api/v1/mailboxes/{mailboxId}/emails/{emailId}")
    suspend fun deleteEmail(
        @Path("mailboxId") mailboxId: String,
        @Path("emailId") emailId: String,
    )

    @POST("/api/v1/mailboxes/{mailboxId}/emails/{emailId}/move")
    suspend fun moveEmail(
        @Path("mailboxId") mailboxId: String,
        @Path("emailId") emailId: String,
        @Body body: MoveEmailRequestDto,
    ): StatusResponseDto

    @GET("/api/v1/mailboxes/{mailboxId}/threads/{threadId}")
    suspend fun getThread(
        @Path("mailboxId") mailboxId: String,
        @Path("threadId") threadId: String,
    ): List<EmailFullDto>

    @POST("/api/v1/mailboxes/{mailboxId}/threads/{threadId}/read")
    suspend fun markThreadRead(
        @Path("mailboxId") mailboxId: String,
        @Path("threadId") threadId: String,
    ): StatusResponseDto

    @POST("/api/v1/mailboxes/{mailboxId}/emails")
    suspend fun sendEmail(
        @Path("mailboxId") mailboxId: String,
        @Body body: SendEmailRequestDto,
    ): SendEmailResponseDto

    @POST("/api/v1/mailboxes/{mailboxId}/emails/{emailId}/reply")
    suspend fun replyEmail(
        @Path("mailboxId") mailboxId: String,
        @Path("emailId") emailId: String,
        @Body body: SendEmailRequestDto,
    ): SendEmailResponseDto

    @POST("/api/v1/mailboxes/{mailboxId}/emails/{emailId}/forward")
    suspend fun forwardEmail(
        @Path("mailboxId") mailboxId: String,
        @Path("emailId") emailId: String,
        @Body body: SendEmailRequestDto,
    ): SendEmailResponseDto

    @GET("/api/v1/mailboxes/{mailboxId}/folders")
    suspend fun getFolders(@Path("mailboxId") mailboxId: String): List<FolderDto>

    @POST("/api/v1/mailboxes/{mailboxId}/folders")
    suspend fun createFolder(
        @Path("mailboxId") mailboxId: String,
        @Body body: FolderNameDto,
    ): FolderDto

    @PUT("/api/v1/mailboxes/{mailboxId}/folders/{folderId}")
    suspend fun updateFolder(
        @Path("mailboxId") mailboxId: String,
        @Path("folderId") folderId: String,
        @Body body: FolderNameDto,
    ): FolderDto

    @DELETE("/api/v1/mailboxes/{mailboxId}/folders/{folderId}")
    suspend fun deleteFolder(
        @Path("mailboxId") mailboxId: String,
        @Path("folderId") folderId: String,
    )

    @GET("/api/v1/mailboxes/{mailboxId}/search")
    suspend fun search(
        @Path("mailboxId") mailboxId: String,
        @Query("query") query: String,
        @Query("folder") folder: String? = null,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("subject") subject: String? = null,
        @Query("date_start") dateStart: String? = null,
        @Query("date_end") dateEnd: String? = null,
        @Query("is_read") isRead: Boolean? = null,
        @Query("is_starred") isStarred: Boolean? = null,
        @Query("has_attachment") hasAttachment: Boolean? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
    ): EmailPageDto

    @GET("/api/v1/mailboxes/{mailboxId}/emails/{emailId}/attachments/{attachmentId}")
    suspend fun downloadAttachment(
        @Path("mailboxId") mailboxId: String,
        @Path("emailId") emailId: String,
        @Path("attachmentId") attachmentId: String,
    ): ByteArray
}
