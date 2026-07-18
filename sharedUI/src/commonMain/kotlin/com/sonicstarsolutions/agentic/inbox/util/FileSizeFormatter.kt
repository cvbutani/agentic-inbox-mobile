package com.sonicstarsolutions.agentic.inbox.util

/** "2.8 MB", "412 KB", "96 B" — the one human-readable size format, used by thread attachment
 * chips and the composer's pending-attachment chips alike. */
fun formatFileSize(bytes: Long): String = when {
    bytes >= 10 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    bytes >= 1024 * 1024 -> {
        val tenths = bytes * 10 / (1024 * 1024)
        "${tenths / 10}.${tenths % 10} MB"
    }
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}
