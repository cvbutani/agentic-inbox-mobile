package com.sonicstarsolutions.agentic.inbox.util

/**
 * Which links tapped inside an email body may be handed to the system. Email HTML is untrusted, so
 * this is an allowlist of user-meaningful schemes; anything else (`javascript:`, `file:`,
 * `intent:`, `data:`, relative or in-page links) is swallowed instead of navigating the body view.
 */
object ExternalLinkPolicy {
    private val externalSchemes = setOf("http", "https", "mailto", "tel")

    fun opensExternally(url: String): Boolean {
        val trimmed = url.trim()
        val colon = trimmed.indexOf(':')
        if (colon <= 0) return false
        return trimmed.substring(0, colon).lowercase() in externalSchemes
    }
}
