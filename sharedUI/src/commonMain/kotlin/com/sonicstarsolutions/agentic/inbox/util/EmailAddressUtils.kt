package com.sonicstarsolutions.agentic.inbox.util

/** Parses email header strings like `"Name <addr@example.dev>"` into plain addresses, for the
 * compose screen's To/Cc/Bcc fields and for prefilling them from a message being replied to. */
object EmailAddressUtils {

    /** One person in an address header: the display name when the header gave one, and the address. */
    data class Person(val name: String?, val address: String) {
        /** What to show for them: the name when known, else the bare address. */
        val label: String get() = name ?: address
    }

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

    /**
     * The people in an address header, in order, each with the name the header gave them. Unlike
     * [parseAddressList] a comma inside a quoted name (`"Lee, Dave" <dave@example.dev>`) does not
     * split the entry.
     */
    fun parseAddresses(raw: String?): List<Person> {
        if (raw.isNullOrBlank()) return emptyList()
        val entries = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in raw) {
            when {
                ch == '"' -> { inQuotes = !inQuotes; current.append(ch) }
                (ch == ',' || ch == ';') && !inQuotes -> { entries += current.toString(); current.clear() }
                else -> current.append(ch)
            }
        }
        entries += current.toString()
        return entries.mapNotNull { entry ->
            val address = extractAddress(entry)
            if (address.isEmpty()) return@mapNotNull null
            val name = nameOf(entry)?.takeIf { it.isNotEmpty() }
            Person(name = name, address = address)
        }
    }

    /**
     * One header string for a sender: `Name <address>`. The server keeps the From name apart from
     * the address, and every screen here already reads the combined form. A name is quoted when it
     * holds something that would end an entry early, and a [sender] that already carries a name
     * is left as it is.
     */
    fun withName(sender: String, name: String?): String {
        val trimmedName = name?.trim().orEmpty()
        if (trimmedName.isEmpty() || sender.contains('<')) return sender
        val needsQuotes = trimmedName.any { it in ",;<>\"" }
        val shown = if (needsQuotes) "\"${trimmedName.replace("\"", "'")}\"" else trimmedName
        return "$shown <$sender>"
    }

    /** What to show for a `"Name <addr@example.dev>"` (or plain address) header string: "You"
     * when it's the current mailbox's own address, otherwise the parsed display name. */
    fun displayName(raw: String, ownEmail: String?): String {
        if (ownEmail != null && extractAddress(raw).equals(ownEmail, ignoreCase = true)) return "You"
        return nameOf(raw)?.takeIf { it.isNotEmpty() } ?: raw.trim()
    }

    /**
     * The From line of a message. Your own messages show the name you sent as, and fall back to
     * "You" only when there was none; everyone else shows by name, or by address without one.
     */
    fun senderLabel(raw: String, ownEmail: String?): String {
        val own = ownEmail != null && extractAddress(raw).equals(ownEmail, ignoreCase = true)
        val name = nameOf(raw)?.takeIf { it.isNotEmpty() }
        return when {
            name != null -> name
            own -> "You"
            else -> raw.trim()
        }
    }

    /** The display-name part of a header entry (quotes removed), or null when it has none. */
    private fun nameOf(raw: String): String? {
        val trimmed = raw.trim()
        val start = trimmed.indexOf('<')
        if (start <= 0) return null
        return trimmed.substring(0, start).trim().removeSurrounding("\"").trim()
    }
}
