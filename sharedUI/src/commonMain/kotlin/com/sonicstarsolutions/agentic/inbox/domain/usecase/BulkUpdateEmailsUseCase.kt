package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.BulkAction
import com.sonicstarsolutions.agentic.inbox.domain.model.BulkOutcome
import com.sonicstarsolutions.agentic.inbox.domain.repository.EmailRepository

class BulkUpdateEmailsUseCase(
    private val repository: EmailRepository,
) {
    /** [folderId] is the destination for [BulkAction.MOVE] and ignored otherwise. */
    suspend operator fun invoke(
        mailboxId: String,
        action: BulkAction,
        emailIds: Collection<String>,
        folderId: String? = null,
    ): BulkOutcome = repository.bulkUpdate(mailboxId, action, emailIds, folderId)
}
