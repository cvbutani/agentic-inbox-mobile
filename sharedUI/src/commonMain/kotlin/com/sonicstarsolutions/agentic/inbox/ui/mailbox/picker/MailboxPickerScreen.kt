package com.sonicstarsolutions.agentic.inbox.ui.mailbox.picker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.ui.WindowWidthClass
import com.sonicstarsolutions.agentic.inbox.ui.components.InitialsAvatar
import com.sonicstarsolutions.agentic.inbox.ui.windowWidthClassFor
import org.koin.compose.viewmodel.koinViewModel

/** Content stops growing past this width on tablets — a grid of cards spaced across a full
 * landscape tablet width reads as sparse, not spacious. */
private val CONTENT_MAX_WIDTH = 960.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailboxPickerScreen(
    onMailboxSelected: (mailboxId: String, mailboxName: String) -> Unit,
    onSignedOut: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: MailboxPickerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.signedOut) {
        if (state.signedOut) {
            viewModel.consumeSignedOut()
            onSignedOut()
        }
    }

    // Computed once at the top, rather than inside the Scaffold's content, so the create-mailbox
    // dialog/sheet choice below can read it too.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthClass = windowWidthClassFor(maxWidth)

        var showCreateDialog by remember { mutableStateOf(false) }
        LaunchedEffect(state.mailboxCreated) {
            if (state.mailboxCreated) {
                showCreateDialog = false
                viewModel.consumeMailboxCreated()
            }
        }

        if (showCreateDialog) {
            // A sheet reads as the natural "more screen real estate for a form" gesture on
            // phones; a dialog stays centred and modest on the room a tablet already has.
            CreateMailboxSheetOrDialog(
                widthClass = widthClass,
                allowedDomains = state.allowedDomains,
                creating = state.creatingMailbox,
                errorMessage = state.createMailboxError,
                onDismiss = {
                    showCreateDialog = false
                    viewModel.consumeCreateMailboxError()
                },
                onCreate = { email, name -> viewModel.createMailbox(email, name) },
            )
        }

        var mailboxToDelete by remember { mutableStateOf<Mailbox?>(null) }
        LaunchedEffect(state.mailboxDeleted) {
            if (state.mailboxDeleted) {
                mailboxToDelete = null
                viewModel.consumeMailboxDeleted()
            }
        }

        mailboxToDelete?.let { mailbox ->
            DeleteMailboxDialog(
                mailboxName = mailbox.name,
                deleting = state.deletingMailbox,
                errorMessage = state.deleteMailboxError,
                onDismiss = {
                    mailboxToDelete = null
                    viewModel.consumeDeleteMailboxError()
                },
                onConfirm = { viewModel.deleteMailbox(mailbox.id) },
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Mailboxes") },
                    actions = {
                        IconButton(onClick = viewModel::signOut) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New mailbox") },
                )
            },
        ) { scaffoldPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(scaffoldPadding),
            ) {
                val horizontalPadding = if (widthClass == WindowWidthClass.COMPACT) 16.dp else 24.dp

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    MailboxPickerContent(
                        state = state,
                        onRefresh = viewModel::refresh,
                        onMailboxSelected = onMailboxSelected,
                        onDeleteRequested = { mailboxToDelete = it },
                        onCreateRequested = { showCreateDialog = true },
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (widthClass == WindowWidthClass.COMPACT) {
                                    Modifier
                                } else {
                                    // On a wide window a full-bleed grid reads as sparse; capping
                                    // and centering it makes the tablet layout feel deliberate
                                    // rather than a phone layout stretched to fit.
                                    Modifier.widthIn(max = CONTENT_MAX_WIDTH)
                                },
                            )
                            .padding(horizontal = horizontalPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun MailboxPickerContent(
    state: MailboxPickerUiState,
    onRefresh: () -> Unit,
    onMailboxSelected: (mailboxId: String, mailboxName: String) -> Unit,
    onDeleteRequested: (Mailbox) -> Unit,
    onCreateRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.loading && state.mailboxes.isEmpty() -> Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }

        state.errorMessage != null && state.mailboxes.isEmpty() -> Box(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
                OutlinedButton(onClick = onRefresh) { Text("Retry") }
            }
        }

        state.mailboxes.isEmpty() -> Box(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Inbox,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    text = "No mailboxes yet",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Create one to start reading and sending mail through your Worker.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onCreateRequested) { Text("New mailbox") }
            }
        }

        else -> PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = onRefresh,
            modifier = modifier,
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.mailboxes, key = { it.id }) { mailbox ->
                    MailboxCard(
                        mailbox = mailbox,
                        onClick = { onMailboxSelected(mailbox.id, mailbox.name) },
                        onDelete = { onDeleteRequested(mailbox) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MailboxCard(
    mailbox: Mailbox,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { showMenu = true }),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InitialsAvatar(name = mailbox.name, modifier = Modifier.size(44.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(mailbox.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    mailbox.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Mailbox options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteMailboxDialog(
    mailboxName: String,
    deleting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!deleting) onDismiss() },
        icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
        title = { Text("Delete mailbox") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Delete \"$mailboxName\"? This cannot be undone.")
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

/**
 * Owns the create-mailbox form's state and picks its container: a [ModalBottomSheet] on phones,
 * where a form benefits from the extra vertical room a sheet can grow into, or an [AlertDialog]
 * on tablets, which already have room to spare and don't need a sheet's full-height affordance.
 * Both containers render the same [CreateMailboxFields] so the two never drift apart.
 */
@Composable
private fun CreateMailboxSheetOrDialog(
    widthClass: WindowWidthClass,
    allowedDomains: List<String>,
    creating: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (email: String, name: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var localPart by rememberSaveable { mutableStateOf("") }
    var selectedDomain by rememberSaveable { mutableStateOf(allowedDomains.firstOrNull().orEmpty()) }
    var freeformEmail by rememberSaveable { mutableStateOf("") }
    var domainMenuExpanded by remember { mutableStateOf(false) }

    val hasDomains = allowedDomains.isNotEmpty()
    val email = if (hasDomains) "$localPart@$selectedDomain" else freeformEmail.trim()
    val canSubmit = name.isNotBlank() &&
        if (hasDomains) localPart.isNotBlank() && selectedDomain.isNotBlank() else freeformEmail.isNotBlank()

    val fields: @Composable () -> Unit = {
        CreateMailboxFields(
            name = name,
            onNameChange = { name = it },
            localPart = localPart,
            onLocalPartChange = { localPart = it },
            freeformEmail = freeformEmail,
            onFreeformEmailChange = { freeformEmail = it },
            selectedDomain = selectedDomain,
            onDomainSelected = { selectedDomain = it },
            allowedDomains = allowedDomains,
            hasDomains = hasDomains,
            domainMenuExpanded = domainMenuExpanded,
            onDomainMenuExpandedChange = { domainMenuExpanded = it },
            creating = creating,
            errorMessage = errorMessage,
        )
    }

    if (widthClass == WindowWidthClass.COMPACT) {
        CreateMailboxBottomSheet(
            creating = creating,
            canSubmit = canSubmit,
            onDismiss = onDismiss,
            onCreate = { onCreate(email, name) },
            fields = fields,
        )
    } else {
        CreateMailboxDialog(
            creating = creating,
            canSubmit = canSubmit,
            onDismiss = onDismiss,
            onCreate = { onCreate(email, name) },
            fields = fields,
        )
    }
}

@Composable
private fun CreateMailboxDialog(
    creating: Boolean,
    canSubmit: Boolean,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    fields: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!creating) onDismiss() },
        icon = { Icon(Icons.Outlined.AlternateEmail, contentDescription = null) },
        title = { Text("New mailbox") },
        text = fields,
        confirmButton = {
            TextButton(onClick = onCreate, enabled = !creating && canSubmit) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateMailboxBottomSheet(
    creating: Boolean,
    canSubmit: Boolean,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    fields: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = { if (!creating) onDismiss() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.AlternateEmail, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("New mailbox", style = MaterialTheme.typography.titleLarge)
            }
            fields()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.End),
            ) {
                TextButton(onClick = onDismiss, enabled = !creating) { Text("Cancel") }
                Button(onClick = onCreate, enabled = !creating && canSubmit) {
                    if (creating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Create")
                    }
                }
            }
        }
    }
}

/** The form fields shared by both the dialog (tablet) and bottom sheet (phone) containers, so
 * they can never drift into two different-looking forms. */
@Composable
private fun CreateMailboxFields(
    name: String,
    onNameChange: (String) -> Unit,
    localPart: String,
    onLocalPartChange: (String) -> Unit,
    freeformEmail: String,
    onFreeformEmailChange: (String) -> Unit,
    selectedDomain: String,
    onDomainSelected: (String) -> Unit,
    allowedDomains: List<String>,
    hasDomains: Boolean,
    domainMenuExpanded: Boolean,
    onDomainMenuExpandedChange: (Boolean) -> Unit,
    creating: Boolean,
    errorMessage: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Display name") },
            singleLine = true,
            enabled = !creating,
            modifier = Modifier.fillMaxWidth(),
        )
        if (hasDomains) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = localPart,
                    onValueChange = onLocalPartChange,
                    label = { Text("Address") },
                    singleLine = true,
                    enabled = !creating,
                    modifier = Modifier.weight(1f),
                )
                Text("@", style = MaterialTheme.typography.bodyLarge)
                if (allowedDomains.size == 1) {
                    Text(
                        text = selectedDomain,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Box {
                        TextButton(onClick = { if (!creating) onDomainMenuExpandedChange(true) }) {
                            Text(selectedDomain)
                        }
                        DropdownMenu(
                            expanded = domainMenuExpanded,
                            onDismissRequest = { onDomainMenuExpandedChange(false) },
                        ) {
                            allowedDomains.forEach { domain ->
                                DropdownMenuItem(
                                    text = { Text(domain) },
                                    onClick = {
                                        onDomainSelected(domain)
                                        onDomainMenuExpandedChange(false)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // No domains loaded (still loading, or the Worker's config genuinely has none) —
            // fall back to a plain free-text address so the flow isn't a dead end.
            OutlinedTextField(
                value = freeformEmail,
                onValueChange = onFreeformEmailChange,
                label = { Text("Email address") },
                singleLine = true,
                enabled = !creating,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
