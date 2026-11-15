package com.sonicstarsolutions.agentic.inbox.domain.model

/** What the server says changed in a mailbox, so a screen knows whether it needs to reload. */
enum class LiveScope { EMAILS, FOLDERS, SCHEDULED, ALL }
