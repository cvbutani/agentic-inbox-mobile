package com.sonicstarsolutions.agentic.inbox.data.network

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * A 400 (rejected request) or 429 (rate limited) from the Worker, with its structured error body
 * already digested. [message] is phrased for the user, so presentation code can show it as is.
 *
 * Only these two statuses are mapped: other statuses keep their raw Ktor exception because
 * callers inspect them (404/501 → [isEndpointUnavailable], Cloudflare Access redirects → the
 * connection check).
 */
class ApiException(
    val status: Int,
    message: String,
    /** Per-field validation messages from a Zod 400, keyed by field name. Empty otherwise. */
    val fieldErrors: Map<String, List<String>> = emptyMap(),
    /** Seconds the server asked the client to wait (429 `Retry-After`), when it said so numerically. */
    val retryAfterSeconds: Int? = null,
) : Exception(message)

private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

/** Maps [t] to an [ApiException] when it's a 400/429 [ResponseException]; any other throwable is
 * returned as is. Reads the response body, hence suspending. */
suspend fun mapApiError(t: Throwable): Throwable {
    if (t !is ResponseException) return t
    val status = t.response.status.value
    if (status != 400 && status != 429) return t

    // A body that can't be read or isn't the Worker's JSON just means "no details" — but a
    // cancellation mid-read must keep propagating.
    val body = try {
        lenientJson.parseToJsonElement(t.response.bodyAsText()) as? JsonObject
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
    val serverMessage = body?.get("error")?.let { (it as? JsonPrimitive)?.contentOrNull }?.takeIf { it.isNotBlank() }

    return when (status) {
        400 -> {
            val fieldErrors = body?.fieldErrors().orEmpty()
            val message = when {
                serverMessage == null -> "The server rejected the request (400)"
                fieldErrors.isEmpty() -> serverMessage
                else -> serverMessage + " — " + fieldErrors.entries.joinToString("; ") { (field, errors) ->
                    "$field: ${errors.joinToString(", ")}"
                }
            }
            ApiException(status, message, fieldErrors)
        }

        else -> {
            val retryAfter = t.response.headers[HttpHeaders.RetryAfter]?.trim()?.toIntOrNull()?.takeIf { it >= 0 }
            val message = serverMessage ?: when (retryAfter) {
                null -> "Too many requests — please try again shortly"
                else -> "Too many requests — please try again in $retryAfter seconds"
            }
            ApiException(status, message, retryAfterSeconds = retryAfter)
        }
    }
}

/** Zod's `error.flatten()` shape: `{"details":{"fieldErrors":{"field":["msg", ...]}}}`. */
private fun JsonObject.fieldErrors(): Map<String, List<String>> {
    val fields = (get("details") as? JsonObject)?.get("fieldErrors") as? JsonObject ?: return emptyMap()
    return fields.mapNotNull { (field, value) ->
        val messages = (value as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()
        if (messages.isEmpty()) null else field to messages
    }.toMap()
}
