package com.sonicstarsolutions.agentic.inbox.util

/** Parses email header strings like `"Name <addr@example.dev>"` into plain addresses, for the
 * compose screen's To/Cc/Bcc fields and for prefilling them from a message being replied to. */
object EmailAddressUtils {

    fun extractAddress(raw: String): String {
        val trimmed = raw.trim()
        val start = trimmed.indexOf('<')
        val end = trimmed.indexOf('>')
        return if (start >= 0 && end > start) trimmed.substring(start + 1, end).trim() else trimmed
    }

    fun parseAddressList(raw: String): List<String> =
        raw.split(',', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { extractAddress(it) }
            .filter { it.isNotEmpty() }

    /** What to show for a `"Name <addr@example.dev>"` (or plain address) header string: "You"
     * when it's the current mailbox's own address, otherwise the parsed display name. */
    fun displayName(raw: String, ownEmail: String?): String {
        if (ownEmail != null && extractAddress(raw).equals(ownEmail, ignoreCase = true)) return "You"
        val trimmed = raw.trim()
        val start = trimmed.indexOf('<')
        return if (start > 0) trimmed.substring(0, start).trim() else trimmed
    }
}
