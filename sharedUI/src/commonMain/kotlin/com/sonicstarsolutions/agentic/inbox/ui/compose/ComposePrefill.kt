package com.sonicstarsolutions.agentic.inbox.ui.compose

import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import com.sonicstarsolutions.agentic.inbox.util.EmailAddressUtils
import com.sonicstarsolutions.agentic.inbox.util.HtmlTextExtractor

data class PrefilledFields(
    val to: String,
    val cc: String,
    val subject: String,
    val body: String,
    val bcc: String = "",
)

/** Derives the compose screen's initial field values from the message being replied to or
 * forwarded — pure and independent of ComposeViewModel so it's directly testable. */
object ComposePrefill {

    fun forReply(original: EmailDetail, replyAll: Boolean, ownEmail: String): PrefilledFields {
        val senderAddress = EmailAddressUtils.extractAddress(original.sender)
        // If the mailbox itself sent this message (e.g. it's the most recent message in the
        // thread, waiting on a reply), "Reply" should address it back to the original
        // recipient(s), not to the mailbox's own address.
        val sentByMailbox = senderAddress.equals(ownEmail, ignoreCase = true)
        val toAddresses = if (sentByMailbox) {
            EmailAddressUtils.parseAddressList(original.recipient).ifEmpty { listOf(senderAddress) }
        } else {
            listOf(senderAddress)
        }

        val cc = if (replyAll) {
            val allParties = EmailAddressUtils.parseAddressList(original.recipient) +
                EmailAddressUtils.parseAddressList(original.cc.orEmpty()) +
                listOf(senderAddress)
            val excluded = (toAddresses.map { it.lowercase() } + ownEmail.lowercase()).toSet()
            allParties.filterNot { it.lowercase() in excluded }.distinct()
        } else {
            emptyList()
        }

        return PrefilledFields(
            to = toAddresses.joinToString(", "),
            cc = cc.joinToString(", "),
            subject = withPrefix(original.subject, "Re:"),
            body = quoteOriginal(original),
        )
    }

    fun forForward(original: EmailDetail): PrefilledFields = PrefilledFields(
        to = "",
        cc = "",
        subject = withPrefix(original.subject, "Fwd:"),
        body = quoteOriginal(original),
    )

    private fun withPrefix(subject: String, prefix: String): String {
        val trimmed = subject.trim()
        return if (trimmed.startsWith(prefix, ignoreCase = true)) trimmed else "$prefix $trimmed"
    }

    private fun quoteOriginal(original: EmailDetail): String {
        val plainBody = HtmlTextExtractor.toPlainText(original.body.orEmpty())
        return """


            ---------- Original message ----------
            From: ${original.sender}
            Date: ${original.date}
            Subject: ${original.subject}

            $plainBody
        """.trimIndent()
    }
}
