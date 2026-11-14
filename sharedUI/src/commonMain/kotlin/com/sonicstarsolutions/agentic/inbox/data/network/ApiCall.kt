package com.sonicstarsolutions.agentic.inbox.data.network

import kotlinx.coroutines.CancellationException

/**
 * Runs [block] and wraps the outcome in [Result], the way the domain layer's repository
 * interfaces expect. Unlike kotlin.runCatching, this rethrows CancellationException instead of
 * swallowing it, so coroutine cancellation still propagates correctly.
 *
 * A rejected, conflicting, rate-limited or failed-delivery response comes back as an [ApiException] with a presentable message; every
 * other failure is passed through unchanged.
 */
suspend inline fun <T> safeApiCall(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(mapApiError(e))
    }
