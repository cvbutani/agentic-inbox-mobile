package com.sonicstarsolutions.agentic.inbox.domain.model

import kotlinx.serialization.Serializable

/**
 * Doubles as the KSafe-persisted shape (hence @Serializable) — a separate storage DTO would be
 * pure ceremony for a 3-field settings value object.
 */
@Serializable
data class Credentials(
    val baseUrl: String = "",
    val clientId: String = "",
    val clientSecret: String = "",
) {
    fun isComplete(): Boolean =
        baseUrl.isNotBlank() && clientId.isNotBlank() && clientSecret.isNotBlank()

    /**
     * Base URL ready for the HTTP layer: trimmed, no trailing slash, and guaranteed to carry a
     * scheme. Ktor's URL parser only extracts a host from strings containing "scheme://" — a bare
     * domain like "my-worker.example.dev" parses as a relative path with an EMPTY host, and Ktor
     * then falls back to localhost:80. Users typically type the domain without a scheme, so
     * default to https.
     */
    fun normalizedBaseUrl(): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        val hasScheme = trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        return if (hasScheme) trimmed else "https://$trimmed"
    }
}
