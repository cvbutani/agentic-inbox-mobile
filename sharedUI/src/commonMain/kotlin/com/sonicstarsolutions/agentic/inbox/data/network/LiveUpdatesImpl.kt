package com.sonicstarsolutions.agentic.inbox.data.network

import com.sonicstarsolutions.agentic.inbox.domain.model.LiveScope
import com.sonicstarsolutions.agentic.inbox.domain.repository.LiveUpdates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Opens one connection to a mailbox's live-update socket and hands every text message to
 * [onText] until the connection closes (by returning) or fails (by throwing). */
fun interface SocketConnector {
    suspend fun connect(mailboxId: String, onText: suspend (String) -> Unit)
}

class LiveUpdatesImpl(
    private val connector: SocketConnector,
) : LiveUpdates {

    override fun changes(mailboxId: String): Flow<LiveScope> = channelFlow {
        var attempt = 0
        while (true) {
            try {
                connector.connect(mailboxId) { text ->
                    parseLiveScope(text)?.let {
                        // Proof the connection works: the next drop starts again from a short wait.
                        attempt = 0
                        send(it)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A refused or dropped connection is routine on a phone; wait and try again.
            }
            delay(reconnectDelay(attempt++))
        }
    }
}

private val liveJson = Json { ignoreUnknownKeys = true; isLenient = true }

/** The server sends `{"type":"changed","scope":"emails|folders|scheduled|all"}`. A scope this app
 * doesn't know counts as everything, so a newer server's notice still triggers a reload. */
internal fun parseLiveScope(text: String): LiveScope? {
    val obj = try {
        liveJson.parseToJsonElement(text) as? JsonObject
    } catch (e: Exception) {
        null
    } ?: return null
    if ((obj["type"] as? JsonPrimitive)?.contentOrNull != "changed") return null
    return when ((obj["scope"] as? JsonPrimitive)?.contentOrNull) {
        "emails" -> LiveScope.EMAILS
        "folders" -> LiveScope.FOLDERS
        "scheduled" -> LiveScope.SCHEDULED
        else -> LiveScope.ALL
    }
}

/** 1s, 2s, 4s … capped at 30s, so a down server is polled gently and a blip is healed fast. */
internal fun reconnectDelay(attempt: Int): Duration {
    val seconds = 1L shl attempt.coerceIn(0, 5)
    return minOf(seconds, 30L).seconds
}
