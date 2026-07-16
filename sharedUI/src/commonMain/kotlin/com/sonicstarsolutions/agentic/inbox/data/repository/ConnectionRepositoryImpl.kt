package com.sonicstarsolutions.agentic.inbox.data.repository

import com.sonicstarsolutions.agentic.inbox.data.network.AgenticInboxApi
import com.sonicstarsolutions.agentic.inbox.data.network.safeApiCall
import com.sonicstarsolutions.agentic.inbox.domain.repository.ConnectionRepository
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

class ConnectionRepositoryImpl(
    private val api: AgenticInboxApi,
) : ConnectionRepository {
    override suspend fun validate(): Result<Unit> =
        safeApiCall { api.getConfig() }
            .fold(
                onSuccess = { Result.success(Unit) },
                onFailure = { t -> Result.failure(describeFailure(t)) },
            )

    // Ktor-specific error inspection stays here so the domain/presentation layers only ever
    // see a Result<Unit> with an already-clean, presentable message.
    private suspend fun describeFailure(t: Throwable): Throwable {
        val message = when (t) {
            is ResponseException -> {
                val code = t.response.status.value
                val location = t.response.headers[HttpHeaders.Location].orEmpty()
                when {
                    // Cloudflare Access answers unauthorized requests with a redirect to its
                    // sign-in page. For a service token that means the token wasn't accepted —
                    // almost always a missing "Service Auth" policy on the Access application.
                    code in 300..399 && location.contains("cloudflareaccess.com") ->
                        "Cloudflare Access rejected the service token and returned its sign-in page.\n\n" +
                            "In Zero Trust → Access → Applications → your Worker's application → " +
                            "Policies, add a policy with action \"Service Auth\" that includes " +
                            "this service token, then retry."

                    else -> {
                        val bodyText = runCatching { t.response.bodyAsText() }.getOrDefault("")
                        val body = bodyText.take(512).trim()
                        if (body.isNotEmpty()) "Server returned HTTP $code\n\n$body" else "Server returned HTTP $code"
                    }
                }
            }
            else -> t.message ?: t::class.simpleName ?: "Unknown error"
        }
        return RuntimeException(message, t)
    }
}
