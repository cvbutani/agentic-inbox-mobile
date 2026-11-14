package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.local.MailboxDao
import com.sonicstarsolutions.agentic.inbox.data.local.MailboxEntity
import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.dto.CreateMailboxDto
import com.sonicstarsolutions.agentic.inbox.data.network.dto.MailboxDto
import com.sonicstarsolutions.agentic.inbox.data.network.safeApiCall
import com.sonicstarsolutions.agentic.inbox.data.network.dto.UpdateMailboxDto
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.model.MailboxSettings
import com.sonicstarsolutions.agentic.inbox.domain.repository.MailboxRepository
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MailboxRepositoryImpl(
    private val api: AgenticInboxApi,
    private val mailboxDao: MailboxDao,
) : MailboxRepository {
    override suspend fun getMailboxes(): Result<List<Mailbox>> {
        val networkResult = safeApiCall { api.listMailboxes().map { it.toDomain() } }
        val mailboxes = networkResult.getOrNull()
        if (mailboxes != null) {
            // The list endpoint never sends `settings`, so every dto here has a null fromName —
            // carry forward whatever a previous single-mailbox GET already cached instead of
            // wiping it out on every refresh.
            val cachedFromNames = mailboxDao.getAll().associate { it.id to it.fromName }
            val merged = mailboxes.map { it.copy(fromName = it.fromName ?: cachedFromNames[it.id]) }
            mailboxDao.deleteAll()
            mailboxDao.upsertAll(merged.map { it.toEntity() })
            return Result.success(merged)
        }

        val cached = mailboxDao.getAll().map { it.toDomain() }
        return if (cached.isNotEmpty()) Result.success(cached) else networkResult
    }

    override suspend fun createMailbox(email: String, name: String): Result<Mailbox> =
        safeApiCall { api.createMailbox(CreateMailboxDto(email = email, name = name)).toDomain() }

    override suspend fun getAllowedDomains(): Result<List<String>> =
        safeApiCall { api.getConfig().domains }

    override suspend fun getMailbox(mailboxId: String): Result<Mailbox> =
        safeApiCall { api.getMailbox(mailboxId).toDomain() }
            .onSuccess { mailboxDao.upsertAll(listOf(it.toEntity())) }

    override suspend fun getMailboxSettings(mailboxId: String): Result<MailboxSettings> =
        safeApiCall { api.getMailbox(mailboxId).settings.asObject().toMailboxSettings() }

    override suspend fun updateMailboxSettings(mailboxId: String, settings: MailboxSettings): Result<MailboxSettings> =
        safeApiCall {
            // The Worker replaces the whole settings object, so start from what it has now.
            val current = api.getMailbox(mailboxId).settings.asObject()
            val saved = api.updateMailbox(mailboxId, UpdateMailboxDto(mergeMailboxSettings(current, settings)))
            // Keep the offline mirror's display name in step with the new fromName.
            mailboxDao.upsertAll(listOf(saved.toDomain().toEntity()))
            saved.settings.asObject().toMailboxSettings()
        }

    override suspend fun deleteMailbox(mailboxId: String): Result<Unit> =
        safeApiCall { api.deleteMailbox(mailboxId) }

    override suspend fun clearCache() = mailboxDao.deleteAll()
}

private fun JsonElement?.asObject(): JsonObject? = this as? JsonObject

private fun MailboxDto.toDomain(): Mailbox {
    val settingsObj = settings?.jsonObject
    val sigObj = settingsObj?.get("signature")?.jsonObject
    return Mailbox(
        id = id,
        email = email,
        name = name,
        fromName = settingsObj?.get("fromName")?.jsonPrimitive?.contentOrNull,
        signatureEnabled = sigObj?.get("enabled")?.jsonPrimitive?.booleanOrNull ?: false,
        signatureText = sigObj?.get("text")?.jsonPrimitive?.contentOrNull,
        signatureHtml = sigObj?.get("html")?.jsonPrimitive?.contentOrNull,
    )
}

private fun Mailbox.toEntity(): MailboxEntity = MailboxEntity(id = id, email = email, name = name, fromName = fromName)

private fun MailboxEntity.toDomain(): Mailbox = Mailbox(id = id, email = email, name = name, fromName = fromName)
