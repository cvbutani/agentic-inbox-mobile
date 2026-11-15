package com.sonicstarsolutions.agentic.inbox.ui.compose

import com.sonicstarsolutions.agentic.inbox.util.EmailAddressUtils

/** Which address a message goes out from, given a mailbox and its aliases. Pure, so it's testable. */
object ComposeFrom {

    /** The mailbox's own address first, then its aliases — each once, compared case-insensitively. */
    fun choices(mailboxEmail: String, aliases: List<String>): List<String> {
        val seen = mutableSetOf(mailboxEmail.lowercase())
        return listOf(mailboxEmail) + aliases.filter { seen.add(it.lowercase()) }
    }

    /**
     * Replying to mail that was sent to one of the aliases defaults to answering from it, so the
     * other side sees the address they wrote to. Anything else defaults to the mailbox itself.
     * [addressedTo] is the original's recipients and cc, in any "Name <addr>" form.
     */
    fun defaultFor(mailboxEmail: String, aliases: List<String>, addressedTo: List<String>): String {
        val addressed = addressedTo.map { EmailAddressUtils.extractAddress(it).lowercase() }.toSet()
        return aliases.firstOrNull { it.lowercase() in addressed } ?: mailboxEmail
    }
}
