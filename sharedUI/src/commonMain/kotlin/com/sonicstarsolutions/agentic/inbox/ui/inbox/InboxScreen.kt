package com.sonicstarsolutions.agentic.inbox.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicstarsolutions.agentic.inbox.domain.model.Draft
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.domain.model.Folder
import com.sonicstarsolutions.agentic.inbox.domain.model.SystemFolders
import com.sonicstarsolutions.agentic.inbox.ui.components.EmailListItem
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    mailboxId: String,
    mailboxName: String,
    modifier: Modifier = Modifier,
    onSwitchMailbox: () -> Unit = {},
    onEmailSelected: (EmailSummary) -> Unit = {},
    onDraftSelected: (Draft) -> Unit = {},
    onComposeNew: () -> Unit = {},
    onSearch: () -> Unit = {},
    /** The email currently open in the detail pane, when this list is one half of the two-pane
     * layout — highlights that row. Null in single-pane, where opening an email leaves the list. */
    openEmailId: String? = null,
    viewModel: InboxViewModel = koinViewModel { parametersOf(mailboxId, mailboxName) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val visibleDrafts = if (state.showingDrafts) state.drafts else emptyList()

    // A swipe-delete is staged, not sent — this Snackbar is the undo window made visible. The
    // window outlasts the Snackbar (see InboxViewModel.UNDO_WINDOW_MILLIS), so a tap on Undo as
    // it fades can't arrive after the delete has already gone.
    LaunchedEffect(state.pendingDelete) {
        val pending = state.pendingDelete ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Deleted \"${pending.email.subject}\"",
            actionLabel = "Undo",
            withDismissAction = false,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoPendingDelete()
        }
    }

    // Nav3 disposes this screen's composition (not just hides it) whenever a child screen like
    // ThreadScreen is pushed on top, and recomposes it fresh on return — so a plain
    // LaunchedEffect(Unit) here re-fires every time the user comes back to the inbox. That's
    // exactly what we want (pick up read/unread, archive, delete, move changes made in the
    // thread view) except on the very first mount, where InboxViewModel's own init already
    // loads page one — hasAppeared survives across that dispose/recompose cycle via
    // rememberSaveable, so only returns trigger the extra refresh, not the first arrival.
    var hasAppeared by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (hasAppeared) {
            viewModel.onRefresh()
        } else {
            hasAppeared = true
        }
    }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    LaunchedEffect(state.folderCreated) {
        if (state.folderCreated) {
            showCreateFolderDialog = false
            viewModel.consumeFolderCreated()
        }
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            creating = state.creatingFolder,
            errorMessage = state.folderActionError,
            onDismiss = {
                showCreateFolderDialog = false
                viewModel.consumeFolderActionError()
            },
            onCreate = { name -> viewModel.createFolder(name) },
        )
    }

    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    LaunchedEffect(state.folderRenamed) {
        if (state.folderRenamed) {
            folderToRename = null
            viewModel.consumeFolderRenamed()
        }
    }

    folderToRename?.let { folder ->
        RenameFolderDialog(
            currentName = folder.name,
            renaming = state.renamingFolder,
            errorMessage = state.folderActionError,
            onDismiss = {
                folderToRename = null
                viewModel.consumeFolderActionError()
            },
            onRename = { name -> viewModel.renameFolder(folder, name) },
        )
    }

    var folderToDelete by remember { mutableStateOf<Folder?>(null) }
    LaunchedEffect(state.folderDeleted) {
        if (state.folderDeleted) {
            folderToDelete = null
            viewModel.consumeFolderActionError()
        }
    }

    // A discarded draft is gone for good — it exists nowhere but this device — so it asks first.
    var draftToDelete by remember { mutableStateOf<Draft?>(null) }
    draftToDelete?.let { draft ->
        AlertDialog(
            onDismissRequest = { draftToDelete = null },
            title = { Text("Discard draft") },
            text = { Text("This draft will be deleted. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDraft(draft.id)
                        draftToDelete = null
                    },
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { draftToDelete = null }) { Text("Keep") }
            },
        )
    }

    // Batch delete has no per-row undo to fall back on (many rows, one action), so it asks first.
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    if (showBatchDeleteDialog) {
        ConfirmDeleteEmailsDialog(
            count = state.selectedEmailIds.size,
            onDismiss = { showBatchDeleteDialog = false },
            onConfirm = {
                showBatchDeleteDialog = false
                viewModel.batchDelete()
            },
        )
    }

    folderToDelete?.let { folder ->
        DeleteFolderDialog(
            folderName = folder.name,
            deleting = state.deletingFolder,
            errorMessage = state.folderActionError,
            onDismiss = {
                folderToDelete = null
                viewModel.consumeFolderActionError()
            },
            onConfirm = { viewModel.deleteFolder(folder) },
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            FolderDrawerContent(
                mailboxName = mailboxName,
                folders = state.folders,
                selectedFolderId = state.currentFolder.id,
                onFolderSelected = { folder ->
                    viewModel.selectFolder(folder)
                    scope.launch { drawerState.close() }
                },
                onSwitchMailbox = {
                    scope.launch { drawerState.close() }
                    onSwitchMailbox()
                },
                onCreateFolder = {
                    scope.launch { drawerState.close() }
                    showCreateFolderDialog = true
                },
                onRenameFolder = { folder ->
                    scope.launch { drawerState.close() }
                    folderToRename = folder
                },
                onDeleteFolder = { folder ->
                    scope.launch { drawerState.close() }
                    folderToDelete = folder
                },
            )
        },
    ) {
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (state.selectionMode) {
                    SelectionTopAppBar(
                        selectedCount = state.selectedEmailIds.size,
                        onClose = viewModel::clearSelection,
                        onSelectAll = viewModel::selectAll,
                        onMarkRead = viewModel::batchMarkAsRead,
                        onMarkUnread = viewModel::batchMarkAsUnread,
                        onArchive = viewModel::batchArchive,
                        onDelete = { showBatchDeleteDialog = true },
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text = state.currentFolder.name,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        actions = {
                            IconButton(onClick = onSearch) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                    )
                }
            },
            floatingActionButton = {
                if (!state.selectionMode) {
                    FloatingActionButton(onClick = onComposeNew) {
                        Icon(Icons.Default.Edit, contentDescription = "Compose")
                    }
                }
            },
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.onRefresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                when {
                    state.loading && state.emails.isEmpty() -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }

                    state.errorMessage != null && state.emails.isEmpty() -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Text(
                                text = state.errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = "Pull down to retry",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    state.emails.isEmpty() && visibleDrafts.isEmpty() -> Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp),
                            )
                            Text(
                                text = "No emails in ${state.currentFolder.name}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Pull down to refresh or check another folder",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        items(visibleDrafts, key = { "draft-${it.id}" }) { draft ->
                            DraftListItem(
                                draft = draft,
                                onClick = { onDraftSelected(draft) },
                                onDelete = { draftToDelete = draft },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }

                        items(state.emails, key = { it.id }) { email ->
                            val emailItem: @Composable () -> Unit = {
                                EmailListItem(
                                    email = email,
                                    onClick = {
                                        if (state.selectionMode) viewModel.toggleSelection(email.id) else onEmailSelected(email)
                                    },
                                    onLongClick = {
                                        if (!state.selectionMode) viewModel.enterSelectionMode(email.id)
                                    },
                                    onToggleStarred = { viewModel.toggleStarred(email) },
                                    selectionMode = state.selectionMode,
                                    selected = if (state.selectionMode) {
                                        email.id in state.selectedEmailIds
                                    } else {
                                        email.id == openEmailId
                                    },
                                )
                            }
                            if (state.selectionMode) {
                                emailItem()
                            } else {
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        when (value) {
                                            SwipeToDismissBoxValue.StartToEnd -> {
                                                viewModel.archiveEmail(email)
                                                true
                                            }
                                            SwipeToDismissBoxValue.EndToStart -> {
                                                viewModel.deleteEmail(email)
                                                true
                                            }
                                            SwipeToDismissBoxValue.Settled -> false
                                        }
                                    },
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = { SwipeActionBackground(dismissState.targetValue) },
                                    content = { emailItem() },
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        item {
                            if (state.loading && state.emails.size < state.totalCount) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                    )
                                }
                            } else if (state.emails.size >= state.totalCount && state.totalCount > 0) {
                                Text(
                                    text = "End of list",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .wrapContentSize(Alignment.Center),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopAppBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onMarkRead: () -> Unit,
    onMarkUnread: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        title = { Text("$selectedCount selected") },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Clear selection")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select all")
            }
            IconButton(onClick = onMarkRead) {
                Icon(Icons.Default.MarkEmailRead, contentDescription = "Mark as read")
            }
            IconButton(onClick = onMarkUnread) {
                Icon(Icons.Default.MarkEmailUnread, contentDescription = "Mark as unread")
            }
            IconButton(onClick = onArchive) {
                Icon(Icons.Default.Archive, contentDescription = "Archive")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    )
}

@Composable
private fun FolderDrawerContent(
    mailboxName: String,
    folders: List<Folder>,
    selectedFolderId: String,
    onFolderSelected: (Folder) -> Unit,
    onSwitchMailbox: () -> Unit,
    onCreateFolder: () -> Unit,
    onRenameFolder: (Folder) -> Unit,
    onDeleteFolder: (Folder) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(modifier = modifier) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSwitchMailbox)
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.AlternateEmail,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(text = mailboxName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Switch mailbox",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            val (systemFolders, customFolders) = folders.partition { it.isSystem }

            systemFolders.forEach { folder ->
                FolderDrawerItem(
                    folder = folder,
                    selected = folder.id == selectedFolderId,
                    onClick = { onFolderSelected(folder) },
                )
            }

            if (customFolders.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Folders",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp),
                )
                customFolders.forEach { folder ->
                    FolderDrawerItem(
                        folder = folder,
                        selected = folder.id == selectedFolderId,
                        onClick = { onFolderSelected(folder) },
                        onRename = { onRenameFolder(folder) },
                        onDelete = { onDeleteFolder(folder) },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                label = { Text("New folder") },
                selected = false,
                onClick = onCreateFolder,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
private fun FolderDrawerItem(
    folder: Folder,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    var showMenu by remember { mutableStateOf(false) }

    NavigationDrawerItem(
        icon = { Icon(imageVector = folderIcon(folder), contentDescription = null) },
        label = { Text(text = folder.name) },
        badge = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (folder.unreadCount > 0) {
                    Badge { Text(folder.unreadCount.toString()) }
                }
                if (onRename != null || onDelete != null) {
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Folder options",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = { showMenu = false; onRename?.invoke() },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = { showMenu = false; onDelete?.invoke() },
                            )
                        }
                    }
                }
            }
        },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeActionBackground(targetValue: SwipeToDismissBoxValue) {
    val (color, icon, alignment) = when (targetValue) {
        SwipeToDismissBoxValue.StartToEnd ->
            Triple(MaterialTheme.colorScheme.primaryContainer, Icons.Default.Archive, Alignment.CenterStart)
        SwipeToDismissBoxValue.EndToStart ->
            Triple(MaterialTheme.colorScheme.errorContainer, Icons.Default.Delete, Alignment.CenterEnd)
        SwipeToDismissBoxValue.Settled -> Triple(MaterialTheme.colorScheme.surface, null, Alignment.Center)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        icon?.let {
            Icon(imageVector = it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

private fun folderIcon(folder: Folder): ImageVector = when (folder.id) {
    SystemFolders.INBOX -> Icons.Default.Inbox
    SystemFolders.DRAFT -> Icons.Default.Drafts
    SystemFolders.SENT -> Icons.AutoMirrored.Filled.Send
    SystemFolders.ARCHIVE -> Icons.Default.Archive
    SystemFolders.SPAM -> Icons.Default.Report
    SystemFolders.TRASH -> Icons.Default.Delete
    else -> Icons.Default.Folder
}

@Composable
private fun CreateFolderDialog(
    creating: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!creating) onDismiss() },
        title = { Text("New folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    enabled = !creating,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name) },
                enabled = !creating && name.isNotBlank(),
            ) {
                if (creating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !creating) { Text("Cancel") }
        },
    )
}

@Composable
private fun RenameFolderDialog(
    currentName: String,
    renaming: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var name by rememberSaveable(currentName) { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = { if (!renaming) onDismiss() },
        title = { Text("Rename folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder name") },
                    singleLine = true,
                    enabled = !renaming,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(name) },
                enabled = !renaming && name.isNotBlank(),
            ) {
                if (renaming) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Rename")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !renaming) { Text("Cancel") }
        },
    )
}

/** A draft's row in the Drafts folder. Deliberately not an [EmailListItem]: a draft has no sender,
 * no read state and nothing to star, and tapping it opens the composer rather than a thread. */
@Composable
private fun DraftListItem(
    draft: Draft,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = draft.subject.ifBlank { "(No subject)" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        supportingContent = {
            Text(
                text = draft.to.ifBlank { "(No recipient)" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Drafts,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Discard draft",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun ConfirmDeleteEmailsDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (count == 1) "Delete email" else "Delete $count emails") },
        text = {
            Text(
                if (count == 1) {
                    "This email will be deleted. This can't be undone."
                } else {
                    "These $count emails will be deleted. This can't be undone."
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DeleteFolderDialog(
    folderName: String,
    deleting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        title = { Text("Delete folder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Delete \"$folderName\"? Emails in this folder won't be deleted.")
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !deleting) {
                if (deleting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !deleting) { Text("Cancel") }
        },
    )
}
