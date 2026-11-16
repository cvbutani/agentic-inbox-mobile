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
     * Which of a domain's mailboxes a new message starts from: the one last sent from if it still
     * exists, otherwise the first alphabetically. Null when the domain has no mailboxes.
     */
    fun defaultMailbox(mailboxes: List<String>, last: String?): String? =
        mailboxes.firstOrNull { it.equals(last, ignoreCase = true) } ?: mailboxes.minByOrNull { it.lowercase() }

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
