package com.sonicstarsolutions.agentic.inbox.util

/**
 * The To/Cc/Bcc fields are one line of text with addresses separated by commas or semicolons.
 * These helpers find the address currently being typed (the text after the last separator) and
 * swap it for a chosen suggestion — pure, so the composer's behaviour is testable.
 */
object RecipientTokens {

    private fun lastSeparator(text: String): Int = maxOf(text.lastIndexOf(','), text.lastIndexOf(';'))

    /** What has been typed since the last separator, trimmed; empty right after one. */
    fun current(text: String): String = text.substring(lastSeparator(text) + 1).trim()

    /** [text] with the word being typed replaced by [address], followed by a separator so the
     * next address can be typed straight away. */
    fun replaceCurrent(text: String, address: String): String {
        val kept = text.substring(0, lastSeparator(text) + 1)
        return if (kept.isEmpty()) "$address, " else "$kept $address, "
    }
}
