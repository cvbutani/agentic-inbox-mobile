package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailAttachment
import com.sonicstarsolutions.agentic.inbox.domain.repository.AttachmentRepository

class DownloadAttachmentUseCase(
    private val repository: AttachmentRepository,
) {
    suspend operator fun invoke(mailboxId: String, emailId: String, attachment: EmailAttachment): Result<String> =
        repository.download(mailboxId, emailId, attachment)
}
