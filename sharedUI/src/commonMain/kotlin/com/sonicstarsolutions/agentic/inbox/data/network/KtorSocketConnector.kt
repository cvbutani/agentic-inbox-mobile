package com.sonicstarsolutions.agentic.inbox.data.network

import com.sonicstarsolutions.agentic.inbox.domain.repository.CredentialsRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.encodeURLPathPart
import io.ktor.websocket.Frame
import io.ktor.websocket.readText

/**
 * The real socket. It rides the app's shared [HttpClient], so the Cloudflare Access service-token
 * headers from its default request go out with the handshake exactly as they do for REST calls.
 */
class KtorSocketConnector(
    private val client: HttpClient,
    private val credentials: CredentialsRepository,
) : SocketConnector {
    override suspend fun connect(mailboxId: String, onText: suspend (String) -> Unit) {
        val url = liveSocketUrl(credentials.state.value.normalizedBaseUrl(), mailboxId) ?: return
        client.webSocket(urlString = url) {
            for (frame in incoming) {
                if (frame is Frame.Text) onText(frame.readText())
            }
        }
    }
}

/** `https://host` + a mailbox → `wss://host/api/v1/mailboxes/<id>/events`; null with no base URL. */
internal fun liveSocketUrl(baseUrl: String, mailboxId: String): String? {
    val base = baseUrl.trim().trimEnd('/')
    if (base.isEmpty()) return null
    val socketBase = when {
        base.startsWith("https://", ignoreCase = true) -> "wss://" + base.substring("https://".length)
        base.startsWith("http://", ignoreCase = true) -> "ws://" + base.substring("http://".length)
        else -> return null
    }
    // '@' is legal in a path segment and the REST calls leave it as is; '/' and spaces are not.
    val id = mailboxId.encodeURLPathPart().replace("%40", "@")
    return "$socketBase/api/v1/mailboxes/$id/events"
}
