package com.sonicstarsolutions.agentic.inbox.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Report
import androidx.compose.ui.graphics.vector.ImageVector
import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders

/** One icon per folder, everywhere a folder appears — drawer, move-to sheet, anywhere else.
 * System folders get their conventional glyphs; custom folders share the generic one. */
fun folderIcon(folder: Folder): ImageVector = when (folder.id) {
    SystemFolders.INBOX -> Icons.Default.Inbox
    SystemFolders.DRAFT -> Icons.Default.Drafts
    SystemFolders.SENT -> Icons.AutoMirrored.Filled.Send
    SystemFolders.ARCHIVE -> Icons.Default.Archive
    SystemFolders.SPAM -> Icons.Default.Report
    SystemFolders.TRASH -> Icons.Default.Delete
    else -> Icons.Default.Folder
}
