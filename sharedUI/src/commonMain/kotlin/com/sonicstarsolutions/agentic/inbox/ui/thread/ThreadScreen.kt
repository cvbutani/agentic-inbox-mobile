package com.sonicstarsolutions.agentic.inbox.ui.thread

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.ReplyAll
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailAttachment
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailDetail
import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.util.EmailHtmlDocumentBuilder
import com.sonicstarsolutions.agentic.inbox.util.EmailHtmlSanitizer
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    mailboxId: String,
    emailId: String,
    threadId: String?,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    viewModel: ThreadViewModel = koinViewModel { parametersOf(mailboxId, emailId, threadId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // One-shot feedback for archive/delete/move/mark-read-unread: show a Snackbar, then (only for
    // actions that remove the email from view) navigate back once it's been seen.
    LaunchedEffect(state.actionResult) {
        val result = state.actionResult ?: return@LaunchedEffect
        val message = when (result) {
            is ThreadActionResult.Success -> result.message
            is ThreadActionResult.Failure -> result.message
        }
        snackbarHostState.showSnackbar(message)
        viewModel.consumeActionResult()
        if (result is ThreadActionResult.Success && result.shouldNavigateBack) {
            onBack()
        }
    }

    val currentMessage = state.messages.firstOrNull { it.id == state.expandedMessageId } ?: state.messages.firstOrNull()

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.messages.firstOrNull()?.subject.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        },
        bottomBar = {
            if (!state.loading && state.errorMessage == null && currentMessage != null) {
                ThreadBottomBar(
                    currentMessageRead = currentMessage.read,
                    folders = state.folders,
                    actionInProgress = state.actionInProgress,
                    onArchive = viewModel::archive,
                    onDelete = viewModel::delete,
                    onMoveTo = viewModel::moveTo,
                    onToggleRead = viewModel::toggleReadState,
                )
            }
        },
    ) { paddingValues ->
        when {
            state.loading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.errorMessage != null -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageCard(
                        message = message,
                        expanded = message.id == state.expandedMessageId,
                        imagesAllowed = state.imagesAllowedFor.contains(message.id),
                        onToggleExpanded = { viewModel.toggleExpanded(message.id) },
                        onAllowImages = { viewModel.allowImages(message.id) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        }
    }
}

/**
 * Reply / Reply all / Forward stay visible but disabled — there's no composer screen yet, and a
 * button that silently does nothing on tap is worse than one that's honestly unavailable.
 * Archive / Delete / Move / Mark read-unread are real, backed by existing API endpoints.
 */
@Composable
private fun ThreadBottomBar(
    currentMessageRead: Boolean,
    folders: List<Folder>,
    actionInProgress: Boolean,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onMoveTo: (String) -> Unit,
    onToggleRead: () -> Unit,
) {
    var showMoveSheet by remember { mutableStateOf(false) }

    if (showMoveSheet) {
        MoveToFolderSheet(
            folders = folders,
            onFolderSelected = { folderId ->
                showMoveSheet = false
                onMoveTo(folderId)
            },
            onDismissRequest = { showMoveSheet = false },
        )
    }

    BottomAppBar {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onArchive, enabled = !actionInProgress) {
                Icon(Icons.Default.Archive, contentDescription = "Archive")
            }
            IconButton(onClick = onDelete, enabled = !actionInProgress) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
            IconButton(
                onClick = { showMoveSheet = true },
                enabled = !actionInProgress && folders.isNotEmpty(),
            ) {
                Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move to folder")
            }
            IconButton(onClick = onToggleRead, enabled = !actionInProgress) {
                Icon(
                    imageVector = if (currentMessageRead) Icons.Default.MarkEmailUnread else Icons.Default.MarkEmailRead,
                    contentDescription = if (currentMessageRead) "Mark as unread" else "Mark as read",
                )
            }
            IconButton(onClick = {}, enabled = false) {
                Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Reply (coming soon)")
            }
            IconButton(onClick = {}, enabled = false) {
                Icon(Icons.AutoMirrored.Filled.ReplyAll, contentDescription = "Reply all (coming soon)")
            }
            IconButton(onClick = {}, enabled = false) {
                Icon(Icons.AutoMirrored.Filled.Forward, contentDescription = "Forward (coming soon)")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveToFolderSheet(
    folders: List<Folder>,
    onFolderSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Text(
            text = "Move to folder",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(folders, key = { it.id }) { folder ->
                ListItem(
                    headlineContent = { Text(folder.name) },
                    leadingContent = {
                        Icon(Icons.Default.Folder, contentDescription = null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFolderSelected(folder.id) },
                )
            }
        }
    }
}

@Composable
private fun MessageCard(
    message: EmailDetail,
    expanded: Boolean,
    imagesAllowed: Boolean,
    onToggleExpanded: () -> Unit,
    onAllowImages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = parseDisplayName(message.sender),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "to ${parseDisplayName(message.recipient)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = message.date.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                val body = message.body.orEmpty()
                val sanitized = EmailHtmlSanitizer.sanitize(body, allowRemoteImages = imagesAllowed)
                val hasBlockedImages = !imagesAllowed && sanitized.contains("data-src=")

                if (hasBlockedImages) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Images blocked for your privacy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = onAllowImages) { Text("Load images") }
                    }
                }

                if (body.isNotBlank()) {
                    val backgroundColor = MaterialTheme.colorScheme.surface
                    val themedHtml = EmailHtmlDocumentBuilder.wrap(
                        bodyHtml = sanitized,
                        textColorHex = MaterialTheme.colorScheme.onSurface.toCssHex(),
                        backgroundColorHex = backgroundColor.toCssHex(),
                        linkColorHex = MaterialTheme.colorScheme.primary.toCssHex(),
                    )
                    HtmlBody(
                        html = themedHtml,
                        backgroundColor = backgroundColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 600.dp),
                    )
                } else {
                    Text(
                        text = "(No content)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                if (message.attachments.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        message.attachments.forEach { attachment ->
                            AttachmentChip(attachment)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentChip(attachment: EmailAttachment) {
    AssistChip(
        onClick = { /* TODO: download/preview — see plan M3 */ },
        label = {
            Text(
                text = attachment.filename,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.AttachFile,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

private fun parseDisplayName(address: String): String {
    val angleBracketIndex = address.indexOf('<')
    if (angleBracketIndex > 0) {
        return address.substring(0, angleBracketIndex).trim()
    }
    return address
}

private fun Color.toCssHex(): String {
    fun component(value: Float) = (value.coerceIn(0f, 1f) * 255).toInt().toString(16).padStart(2, '0')
    return "#${component(red)}${component(green)}${component(blue)}"
}
