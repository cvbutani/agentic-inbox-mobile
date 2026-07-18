package com.sonicstarsolutions.agentic.inbox.ui.thread

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
import com.sonicstarsolutions.agentic.inbox.ui.components.InitialsAvatar
import com.sonicstarsolutions.agentic.inbox.ui.components.SkeletonEmailRow
import com.sonicstarsolutions.agentic.inbox.ui.components.StatusPane
import com.sonicstarsolutions.agentic.inbox.ui.components.folderIcon
import com.sonicstarsolutions.agentic.inbox.util.HtmlTextExtractor
import com.sonicstarsolutions.agentic.inbox.util.EmailAddressUtils
import com.sonicstarsolutions.agentic.inbox.util.EmailTimeFormatter
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
    /** True when this screen is the detail pane of a split, where [onBack] empties the pane
     * rather than popping a route — a Close (X) glyph says that; a back arrow lies. */
    useCloseAffordance: Boolean = false,
    onReply: (emailId: String) -> Unit = {},
    onReplyAll: (emailId: String) -> Unit = {},
    onForward: (emailId: String) -> Unit = {},
    onEditDraft: (draftMessageId: String) -> Unit = {},
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
    // Archive/delete/move act on whichever message is currently open, same as before — only their
    // location moved (top bar instead of a bottom bar). None of them make sense on a draft.
    val showThreadActions = !state.loading && state.errorMessage == null &&
        currentMessage != null && currentMessage.folderId != SystemFolders.DRAFT

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete email") },
            text = { Text("This email will be deleted. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showMoveSheet) {
        MoveToFolderSheet(
            // Offering the folder the message is already in would be a no-op row.
            folders = state.folders.filter { it.id != currentMessage?.folderId },
            onFolderSelected = { folderId ->
                showMoveSheet = false
                viewModel.moveTo(folderId)
            },
            onDismissRequest = { showMoveSheet = false },
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                // The subject lives in the content as the thread's title, where it can wrap —
                // a one-line ellipsized bar title hides exactly the words that matter.
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        if (useCloseAffordance) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        } else {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (showThreadActions) {
                        IconButton(onClick = viewModel::archive, enabled = !state.actionInProgress) {
                            Icon(Icons.Default.Archive, contentDescription = "Archive")
                        }
                        IconButton(onClick = { showDeleteDialog = true }, enabled = !state.actionInProgress) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                        IconButton(
                            onClick = { showMoveSheet = true },
                            enabled = !state.actionInProgress && state.folders.isNotEmpty(),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move to folder")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        },
        bottomBar = {
            // The only bottom bar left is the draft one — everything else moved up to the top bar
            // or onto each message row.
            if (!state.loading && state.errorMessage == null && currentMessage?.folderId == SystemFolders.DRAFT) {
                DraftBottomBar(
                    actionInProgress = state.actionInProgress,
                    onSend = { viewModel.sendDraft(currentMessage.id) },
                    onEdit = { onEditDraft(currentMessage.id) },
                )
            }
        },
    ) { paddingValues ->
        // Capped and centered on wide windows: a message card stretched across a full tablet
        // runs body text far past a readable line length. In the inbox split the pane already
        // constrains this; the cap only bites on the pushed route (e.g. opened from search).
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.TopCenter,
        ) {
        Box(modifier = Modifier.fillMaxSize().widthIn(max = CONTENT_MAX_WIDTH)) {
        when {
            // Skeleton message cards on the same grid as the real ones — the screen looks like
            // itself while loading instead of showing a bare spinner.
            state.loading -> Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(2) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        SkeletonEmailRow()
                    }
                }
            }

            state.errorMessage != null -> StatusPane(
                icon = Icons.Default.ErrorOutline,
                title = "Couldn't load this conversation",
                detail = state.errorMessage,
            ) {
                Button(onClick = viewModel::retry) { Text("Retry") }
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.messages.firstOrNull()?.subject?.takeIf { it.isNotBlank() }?.let { subject ->
                    item(key = "subject") {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }

                items(state.messages, key = { it.id }) { message ->
                    MessageCard(
                        modifier = Modifier.padding(horizontal = 16.dp).animateItem(),
                        message = message,
                        expanded = message.id == state.expandedMessageId,
                        imagesAllowed = state.imagesAllowedFor.contains(message.id),
                        actionInProgress = state.actionInProgress,
                        mailboxEmail = state.mailboxEmail,
                        downloadingAttachmentIds = state.downloadingAttachmentIds,
                        onToggleExpanded = { viewModel.toggleExpanded(message.id) },
                        onAllowImages = { viewModel.allowImages(message.id) },
                        onToggleStarred = { viewModel.toggleStarred(message.id) },
                        onToggleRead = { viewModel.toggleReadState(message.id) },
                        onReply = { onReply(message.id) },
                        onReplyAll = { onReplyAll(message.id) },
                        onForward = { onForward(message.id) },
                        onOpenAttachment = { viewModel.openAttachment(message.id, it) },
                        onShareAttachment = { viewModel.shareAttachment(message.id, it) },
                    )
                }
            }
        }
        }
        }
    }
}

/** Content stops growing past this width on tablets — message bodies stay a readable measure. */
private val CONTENT_MAX_WIDTH = 840.dp

@Composable
private fun DraftBottomBar(
    actionInProgress: Boolean,
    onSend: () -> Unit,
    onEdit: () -> Unit,
) {
    BottomAppBar {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onEdit,
                enabled = !actionInProgress,
                modifier = Modifier.weight(1f),
            ) { Text("Edit") }
            Button(
                onClick = onSend,
                enabled = !actionInProgress,
                modifier = Modifier.weight(1f),
            ) {
                if (actionInProgress) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Send")
                }
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
                        Icon(folderIcon(folder), contentDescription = null)
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
    actionInProgress: Boolean,
    mailboxEmail: String?,
    downloadingAttachmentIds: Set<String>,
    onToggleExpanded: () -> Unit,
    onAllowImages: () -> Unit,
    onToggleStarred: () -> Unit,
    onToggleRead: () -> Unit,
    onReply: () -> Unit,
    onReplyAll: () -> Unit,
    onForward: () -> Unit,
    onOpenAttachment: (attachmentId: String) -> Unit,
    onShareAttachment: (attachmentId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDraft = message.folderId == SystemFolders.DRAFT
    val fromText = EmailAddressUtils.displayName(message.sender, mailboxEmail)
    val toText = EmailAddressUtils.displayName(message.recipient, mailboxEmail)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: avatar beside the text block — the same identity language as the inbox
            // rows, drawer, and mailbox picker. The whole header toggles expansion; no chevron
            // (the visible body is the state).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                InitialsAvatar(
                    name = fromText,
                    modifier = Modifier.size(40.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Line 1: from, then the star (same place in both states — a control the
                    // user just aimed at must not teleport on tap), then read-toggle when open.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = fromText,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (isDraft) {
                            DraftLabel()
                        }
                        // No size overrides: IconButton's default 48dp minimum touch target is
                        // the accessibility floor.
                        IconButton(onClick = onToggleStarred) {
                            Icon(
                                imageVector = if (message.starred) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (message.starred) "Unstar" else "Star",
                                tint = if (message.starred) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        if (expanded && !isDraft) {
                            IconButton(onClick = onToggleRead, enabled = !actionInProgress) {
                                Icon(
                                    imageVector = if (message.read) Icons.Default.MarkEmailUnread else Icons.Default.MarkEmailRead,
                                    contentDescription = if (message.read) "Mark as unread" else "Mark as read",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    // Line 2: to, then (collapsed only) the time — the full date moves to its
                    // own line once expanded.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "to $toText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (!expanded) {
                            Text(
                                text = EmailTimeFormatter.format(message.date),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // Line 3 (expanded only): the full date and time.
                    if (expanded) {
                        Text(
                            text = EmailTimeFormatter.formatAbsolute(message.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Collapsed messages get one preview line — in a long thread, a header
                    // with no content hint is unidentifiable without expanding it.
                    if (!expanded) {
                        val preview = HtmlTextExtractor.toPlainText(message.body.orEmpty())
                        if (preview.isNotBlank()) {
                            Text(
                                text = preview,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Animated, not an instant pop — expanding a message is the screen's defining
            // interaction and should read as motion, not a glitch.
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                val body = message.body.orEmpty()
                val sanitized = EmailHtmlSanitizer.sanitize(body, allowRemoteImages = imagesAllowed)
                val hasBlockedImages = !imagesAllowed && sanitized.contains("data-src=")

                if (hasBlockedImages) {
                    // A calm tonal banner with a text action — a filled Button here was the
                    // loudest element in the message for what is only a notice.
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
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
                            TextButton(onClick = onAllowImages) { Text("Load images") }
                        }
                    }
                }

                if (body.isNotBlank()) {
                    // The card's own color — theming the body to plain `surface` split the card
                    // into two visible background tones in dark mode.
                    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow
                    val themedHtml = EmailHtmlDocumentBuilder.wrap(
                        bodyHtml = sanitized,
                        textColorHex = MaterialTheme.colorScheme.onSurface.toCssHex(),
                        backgroundColorHex = backgroundColor.toCssHex(),
                        linkColorHex = MaterialTheme.colorScheme.primary.toCssHex(),
                    )
                    // No height constraint: HtmlBody measures its own content and sizes to it, so
                    // the message reads as part of the thread rather than as a scroller inside it.
                    HtmlBody(
                        html = themedHtml,
                        backgroundColor = backgroundColor,
                        modifier = Modifier.fillMaxWidth(),
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
                    // LazyRow, not Row: a plain Row pushes the third-and-later attachments off
                    // the card edge with no way to reach them.
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(message.attachments, key = { it.id }) { attachment ->
                            AttachmentChip(
                                attachment = attachment,
                                downloading = attachment.id in downloadingAttachmentIds,
                                onOpen = { onOpenAttachment(attachment.id) },
                                onShare = { onShareAttachment(attachment.id) },
                            )
                        }
                    }
                }

                // None of these make sense on a message that hasn't been sent yet — a draft gets
                // Send/Edit instead, from the screen-level bottom bar.
                if (!isDraft) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    // Labeled, start-aligned — the three actions users came for shouldn't
                    // demand icon literacy or be stretched across the card as filler.
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onReply, enabled = !actionInProgress) {
                            Icon(
                                Icons.AutoMirrored.Filled.Reply,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text("Reply", modifier = Modifier.padding(start = 6.dp))
                        }
                        TextButton(onClick = onReplyAll, enabled = !actionInProgress) {
                            Icon(
                                Icons.AutoMirrored.Filled.ReplyAll,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text("Reply all", modifier = Modifier.padding(start = 6.dp))
                        }
                        TextButton(onClick = onForward, enabled = !actionInProgress) {
                            Icon(
                                Icons.AutoMirrored.Filled.Forward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text("Forward", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun DraftLabel() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = "Draft",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** One attachment: tap opens it (downloading first if needed), long-press shares it. The
 * spinner replaces the paperclip while the bytes are in flight. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AttachmentChip(
    attachment: EmailAttachment,
    downloading: Boolean,
    onOpen: () -> Unit,
    onShare: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onOpen, onLongClick = onShare),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (downloading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column {
                Text(
                    text = attachment.filename,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp),
                )
                Text(
                    text = formatAttachmentSize(attachment.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatAttachmentSize(bytes: Long): String = when {
    bytes >= 10 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    bytes >= 1024 * 1024 -> {
        val tenths = bytes * 10 / (1024 * 1024)
        "${tenths / 10}.${tenths % 10} MB"
    }
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

private fun Color.toCssHex(): String {
    fun component(value: Float) = (value.coerceIn(0f, 1f) * 255).toInt().toString(16).padStart(2, '0')
    return "#${component(red)}${component(green)}${component(blue)}"
}
