package com.sonicstarsolutions.agentic.inbox.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes

@OptIn(ExperimentalForeignApi::class)
class IosAttachmentStore : AttachmentStore {

    private val fileManager = NSFileManager.defaultManager

    private fun rootDirectory(): String {
        val url = fileManager.URLForDirectory(
            directory = NSCachesDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        return requireNotNull(url?.path) { "Could not resolve the iOS caches directory" } + "/attachments"
    }

    private fun pathFor(attachmentId: String, filename: String): String =
        "${rootDirectory()}/$attachmentId/${sanitizeAttachmentFilename(filename)}"

    override suspend fun cachedPath(attachmentId: String, filename: String, expectedSize: Long): String? =
        withContext(Dispatchers.IO) {
            val path = pathFor(attachmentId, filename)
            if (!fileManager.fileExistsAtPath(path)) return@withContext null
            val attributes = fileManager.attributesOfItemAtPath(path, error = null)
            val size = (attributes?.get(NSFileSize) as? NSNumber)?.longLongValue
            if (size == expectedSize) path else null
        }

    override suspend fun write(attachmentId: String, filename: String, bytes: ByteArray): String =
        withContext(Dispatchers.IO) {
            val path = pathFor(attachmentId, filename)
            val directory = path.substringBeforeLast('/')
            fileManager.createDirectoryAtPath(directory, withIntermediateDirectories = true, attributes = null, error = null)
            val data = if (bytes.isEmpty()) {
                NSData()
            } else {
                bytes.usePinned { pinned ->
                    NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
                }
            }
            check(data.writeToFile(path, atomically = true)) { "Failed to write attachment to $path" }
            path
        }
}
