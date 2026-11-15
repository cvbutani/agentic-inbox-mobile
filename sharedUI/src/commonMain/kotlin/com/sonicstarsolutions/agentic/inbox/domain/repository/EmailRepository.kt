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

    /** Emails carrying [labelId], newest first, across folders (Trash excluded by the server). Not cached. */
    suspend fun getEmailsByLabel(mailboxId: String, labelId: String, page: Int, limit: Int): Result<EmailPage>

    suspend fun moveEmail(mailboxId: String, emailId: String, folderId: String): Result<Unit>
    /** Applies [action] to every id in as few requests as the server allows. Never throws: ids
     * that could not be updated come back in [BulkOutcome.failed]. [folderId] is the target of a move and [labelId] the label for LABEL/UNLABEL. */
    suspend fun bulkUpdate(
        mailboxId: String,
        action: BulkAction,
        emailIds: Collection<String>,
        folderId: String? = null,
        labelId: String? = null,
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
