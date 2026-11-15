package com.sonicstarsolutions.agentic.inbox.domain.repository

import com.sonicstarsolutions.agentic.inbox.domain.model.BulkAction
import com.sonicstarsolutions.agentic.inbox.domain.model.BulkOutcome
import com.sonicstarsolutions.agentic.inbox.domain.model.ComposeEmailRequest
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailPage
import com.sonicstarsolutions.agentic.inbox.domain.model.SearchQuery

interface EmailRepository {
    suspend fun getEmails(
        mailboxId: String,
        folder: String,
        page: Int,
        limit: Int,
    ): Result<EmailPage>

    suspend fun search(
        mailboxId: String,
        query: SearchQuery,
        page: Int,
        limit: Int,
    ): Result<EmailPage>

    /**
     * One merged list across every mailbox on [domain], newest first, one row per conversation.
     * Pages by [cursor] (null for the first page; the previous page's [EmailPage.nextCursor]
     * after that). The first page is cached under each row's own mailbox, and is what an
     * offline call falls back to.
     */
    suspend fun getDomainEmails(domain: String, folder: String, cursor: String?, limit: Int): Result<EmailPage>

    /** Search across every mailbox on [domain]; same paging as [getDomainEmails]. Not cached. */
    suspend fun searchDomain(domain: String, query: SearchQuery, cursor: String?, limit: Int): Result<EmailPage>

    /** Emails carrying [labelId], newest first, across folders (Trash excluded by the server). Not cached. */
    suspend fun getEmailsByLabel(mailboxId: String, labelId: String, page: Int, limit: Int): Result<EmailPage>

    suspend fun moveEmail(mailboxId: String, emailId: String, folderId: String): Result<Unit>
    /** Applies [action] to every id in as few requests as the server allows. Never throws: ids
     * that could not be updated come back in [BulkOutcome.failed], under the row's id.
     * [conversations] maps a row id to its thread id for rows that stand for a whole conversation:
     * those are acted on by thread (every message of it in [folder], the list being viewed). [folderId] is the target of a move and [labelId] the label for LABEL/UNLABEL. */
    suspend fun bulkUpdate(
        mailboxId: String,
        action: BulkAction,
        emailIds: Collection<String>,
        folderId: String? = null,
        labelId: String? = null,
        conversations: Map<String, String> = emptyMap(),
        folder: String? = null,
    ): BulkOutcome

    suspend fun deleteEmail(mailboxId: String, emailId: String): Result<Unit>
    suspend fun setRead(mailboxId: String, emailId: String, read: Boolean): Result<Unit>
    suspend fun setStarred(mailboxId: String, emailId: String, starred: Boolean): Result<Unit>
    suspend fun markThreadRead(mailboxId: String, threadId: String): Result<Unit>

    suspend fun sendEmail(mailboxId: String, request: ComposeEmailRequest): Result<Unit>
    suspend fun replyEmail(mailboxId: String, emailId: String, request: ComposeEmailRequest): Result<Unit>
    suspend fun forwardEmail(mailboxId: String, emailId: String, request: ComposeEmailRequest): Result<Unit>

    /** Wipes every locally cached email for every mailbox/folder. See [MailboxRepository.clearCache]. */
    suspend fun clearCache()
}
