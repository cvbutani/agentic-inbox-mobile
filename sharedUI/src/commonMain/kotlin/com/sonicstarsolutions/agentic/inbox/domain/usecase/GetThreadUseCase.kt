package com.sonicstarsolutions.agentic.inbox.domain.usecase

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import com.sonicstarsolutions.agentic.inbox.domain.repository.ThreadRepository

class GetThreadUseCase(
    private val repository: ThreadRepository,
) {
    suspend operator fun invoke(mailboxId: String, emailId: String, threadId: String?): Result<List<EmailDetail>> =
        repository.getThread(mailboxId, emailId, threadId)
}
