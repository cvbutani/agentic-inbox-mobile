package com.sonicstarsolutions.agentic.inbox.data.network

import com.sonicstarsolutions.agentic.inbox.data.settings.Credentials
import com.sonicstarsolutions.agentic.inbox.data.settings.CredentialsRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Builds a Ktor client whose `defaultRequest` is bound to the base URL from
 * the credentials repository and attaches the Access service-token headers
 * per call.
 *
 * Credentials are read at request time (defaultRequest runs each call), so
 * swapping them via [CredentialsRepository.save] takes effect immediately
 * without rebuilding the client. M0 surfaces a raw HTTP/transport error
 * through to the onboarding screen; the typed `SessionExpired` event lands
 * with the rest of the app surface in M1.
 */
internal fun buildHttpClient(
    engine: HttpClientEngine,
    credentials: CredentialsRepository,
    json: Json = DefaultJson,
): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) { json(json) }

    defaultRequest {
        val snapshot = credentials.current()
        url.takeFrom(snapshot.baseUrl.trim().trimEnd('/'))
        attachAccessHeaders(snapshot)
    }
}

internal val DefaultJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = false
}

private fun DefaultRequest.DefaultRequestBuilder.attachAccessHeaders(c: Credentials) {
    // Service-token auth (Option A from the plan). Header names match what
    // Cloudflare Access expects on requests carrying a service token.
    if (c.clientId.isNotBlank()) header("CF-Access-Client-Id", c.clientId)
    if (c.clientSecret.isNotBlank()) header("CF-Access-Client-Secret", c.clientSecret)
}
