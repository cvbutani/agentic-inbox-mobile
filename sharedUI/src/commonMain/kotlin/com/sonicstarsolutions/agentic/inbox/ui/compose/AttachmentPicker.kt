package com.sonicstarsolutions.agentic.inbox.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

/**
 * The app's ONLY FileKit touchpoint — the pre-1.0 dependency is quarantined here so a breaking
 * release (or a swap to hand-rolled pickers) touches exactly this file. Returns a launch lambda;
 * each picked file is read off the UI thread and delivered through [onFilePicked].
 */
@Composable
fun rememberAttachmentPicker(
    onFilePicked: (filename: String, mimeType: String, bytes: ByteArray) -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    val launcher = rememberFilePickerLauncher(
        type = FileKitType.File(),
        mode = FileKitMode.Multiple(),
    ) { files: List<PlatformFile>? ->
        files?.forEach { file ->
            scope.launch {
                onFilePicked(file.name, mimeTypeForFilename(file.name), file.readBytes())
            }
        }
    }
    return { launcher.launch() }
}

/** Extension-based MIME lookup: the send schema wants a type, pickers don't reliably give one
 * cross-platform, and receivers treat octet-stream as "just download it" — an acceptable floor. */
internal fun mimeTypeForFilename(filename: String): String = when (filename.substringAfterLast('.', "").lowercase()) {
    "jpg", "jpeg" -> "image/jpeg"
    "png" -> "image/png"
    "gif" -> "image/gif"
    "webp" -> "image/webp"
    "heic" -> "image/heic"
    "pdf" -> "application/pdf"
    "txt" -> "text/plain"
    "csv" -> "text/csv"
    "html", "htm" -> "text/html"
    "zip" -> "application/zip"
    "mp4" -> "video/mp4"
    "mp3" -> "audio/mpeg"
    "doc" -> "application/msword"
    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "xls" -> "application/vnd.ms-excel"
    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    "ppt" -> "application/vnd.ms-powerpoint"
    "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    else -> "application/octet-stream"
}
