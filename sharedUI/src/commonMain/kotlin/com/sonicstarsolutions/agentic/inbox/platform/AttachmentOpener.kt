package com.sonicstarsolutions.agentic.inbox.platform

/**
 * Hands a locally cached attachment file to the platform: a viewer for [open], the system
 * share sheet for [share]. Failure means the platform had no way to handle the file (e.g. no
 * app installed for the type) — the caller surfaces that, not this interface.
 */
interface AttachmentOpener {
    fun open(path: String, mimeType: String): Result<Unit>
    fun share(path: String, mimeType: String): Result<Unit>
}
