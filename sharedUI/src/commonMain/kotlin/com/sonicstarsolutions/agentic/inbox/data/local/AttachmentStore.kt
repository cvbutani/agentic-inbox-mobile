package com.sonicstarsolutions.agentic.inbox.data.local

/**
 * On-disk cache for downloaded attachments, living in the platform's *cache* directory so the
 * OS may reclaim the space — an attachment can always be re-downloaded. Attachments are
 * immutable (an id's bytes never change), so validity is simply "a file of the expected size
 * exists"; no TTLs or ETags.
 */
interface AttachmentStore {
    /** Absolute path of a complete cached copy (size matches), or null if absent/partial. */
    suspend fun cachedPath(attachmentId: String, filename: String, expectedSize: Long): String?

    /** Persists [bytes] and returns the absolute path they now live at. */
    suspend fun write(attachmentId: String, filename: String, bytes: ByteArray): String
}

/** Mirrors the Worker's own R2-key sanitization: a server-supplied filename must never be able
 * to walk out of the cache directory or smuggle path separators into it. */
fun sanitizeAttachmentFilename(filename: String): String {
    val safe = filename.replace(Regex("""[/\\:*?"<>|\x00-\x1f]"""), "_")
    return safe.ifBlank { "untitled" }
}
