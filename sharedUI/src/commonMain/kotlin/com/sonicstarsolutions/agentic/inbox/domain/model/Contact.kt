package com.sonicstarsolutions.agentic.inbox.domain.model

/** Someone the mailbox has corresponded with; [count] is how many messages involve them. */
data class Contact(val address: String, val count: Int)
