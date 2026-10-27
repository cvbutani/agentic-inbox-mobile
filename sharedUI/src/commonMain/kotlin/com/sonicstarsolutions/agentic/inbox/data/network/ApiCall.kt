package com.sonicstarsolutions.agentic.inbox.data.network

import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException

/**
 * Runs [block] and wraps the outcome in [Result], the way the domain layer's repository
 * interfaces expect. Unlike kotlin.runCatching, this rethrows CancellationException instead of
 * swallowing it, so coroutine cancellation still propagates correctly.
 */
/** True when the Worker answered 404/501 — the endpoint doesn't exist on this server (older
 * Worker) or the feature behind it is switched off. Distinct from a genuine failure of an
 * endpoint that does exist, which must stay visible to the user. */
fun Throwable.isEndpointUnavailable(): Boolean =
    this is ResponseException && response.status.value.let { it == 404 || it == 501 }

suspend inline fun <T> safeApiCall(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
