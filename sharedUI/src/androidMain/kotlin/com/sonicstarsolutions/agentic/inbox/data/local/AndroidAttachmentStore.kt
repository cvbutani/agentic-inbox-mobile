package com.sonicstarsolutions.agentic.inbox.data.local

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidAttachmentStore(context: Context) : AttachmentStore {

    private val root = File(context.applicationContext.cacheDir, "attachments")

    private fun fileFor(attachmentId: String, filename: String): File =
        File(File(root, attachmentId), sanitizeAttachmentFilename(filename))

    override suspend fun cachedPath(attachmentId: String, filename: String, expectedSize: Long): String? =
        withContext(Dispatchers.IO) {
            val file = fileFor(attachmentId, filename)
            if (file.exists() && file.length() == expectedSize) file.absolutePath else null
        }

    override suspend fun write(attachmentId: String, filename: String, bytes: ByteArray): String =
        withContext(Dispatchers.IO) {
            val file = fileFor(attachmentId, filename)
            file.parentFile?.mkdirs()
            file.writeBytes(bytes)
            file.absolutePath
        }
}
