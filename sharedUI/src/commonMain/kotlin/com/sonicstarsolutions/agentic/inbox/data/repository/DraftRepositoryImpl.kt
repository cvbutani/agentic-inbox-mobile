package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.local.DraftDao
import com.sonicstarsolutions.agentic.inbox.data.local.DraftEntity
import com.sonicstarsolutions.agentic.inbox.domain.model.Draft
import com.sonicstarsolutions.agentic.inbox.domain.repository.DraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DraftRepositoryImpl(
    private val draftDao: DraftDao,
) : DraftRepository {

    override suspend fun save(draft: Draft) = draftDao.upsert(draft.toEntity())

    override suspend fun get(id: String): Draft? = draftDao.getById(id)?.toDomain()

    override fun observe(mailboxId: String): Flow<List<Draft>> =
        draftDao.observeForMailbox(mailboxId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun delete(id: String) = draftDao.deleteById(id)
}

private fun Draft.toEntity(): DraftEntity = DraftEntity(
    id = id,
    mailboxId = mailboxId,
    to = to,
    cc = cc,
    bcc = bcc,
    subject = subject,
    body = body,
    mode = mode,
    originalEmailId = originalEmailId,
    threadId = threadId,
    updatedAt = updatedAt,
)

private fun DraftEntity.toDomain(): Draft = Draft(
    id = id,
    mailboxId = mailboxId,
    to = to,
    cc = cc,
    bcc = bcc,
    subject = subject,
    body = body,
    mode = mode,
    originalEmailId = originalEmailId,
    threadId = threadId,
    updatedAt = updatedAt,
)
