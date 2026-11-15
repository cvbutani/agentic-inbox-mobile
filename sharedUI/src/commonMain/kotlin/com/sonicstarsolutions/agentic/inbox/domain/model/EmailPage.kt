package com.sonicstarsolutions.agentic.inbox.domain.model

data class EmailPage(
    val emails: List<EmailSummary>,
    val totalCount: Int,
    /** Where the next page of a domain-wide list starts; null on the last page and in any
     * single-mailbox list, which pages by number instead. */
    val nextCursor: String? = null,
)
