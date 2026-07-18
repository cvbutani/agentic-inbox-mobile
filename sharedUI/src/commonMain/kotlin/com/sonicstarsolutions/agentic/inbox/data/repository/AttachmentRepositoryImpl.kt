package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.local.AttachmentStore
import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailAttachment
import com.sonicstarsolutions.agentic.inbox.domain.repository.AttachmentRepository
import kotlinx.coroutines.CancellationException

class AttachmentRepositoryImpl(
    private val api: AgenticInboxApi,
    private val store: AttachmentStore,
) : AttachmentRepository {

    override suspend fun download(mailboxId: String, emailId: String, attachment: EmailAttachment): Result<String> {
        val cached = store.cachedPath(attachment.id, attachment.filename, attachment.size)
        if (cached != null) return Result.success(cached)
        return try {
            val bytes = api.downloadAttachment(mailboxId, emailId, attachment.id)
            Result.success(store.write(attachment.id, attachment.filename, bytes))
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
