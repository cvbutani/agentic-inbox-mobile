package com.sonicstarsolutions.agentic.inbox.ui.mailbox.picker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.domain.model.displayName
import com.sonicstarsolutions.agentic.inbox.theme.ListRowDimens
import com.sonicstarsolutions.agentic.inbox.ui.WindowWidthClass
import com.sonicstarsolutions.agentic.inbox.ui.components.AppBarTitle
import com.sonicstarsolutions.agentic.inbox.ui.components.ErrorBanner
import com.sonicstarsolutions.agentic.inbox.ui.components.InitialsAvatar
import com.sonicstarsolutions.agentic.inbox.ui.components.SkeletonEmailRow
import com.sonicstarsolutions.agentic.inbox.ui.components.StatusPane
import com.sonicstarsolutions.agentic.inbox.ui.windowWidthClassFor
import org.koin.compose.viewmodel.koinViewModel

/** Content stops growing past this width on tablets — a list row stretched across a full
 * landscape tablet puts a mailbox's name and its menu a metre apart. */
private val CONTENT_MAX_WIDTH = 720.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailboxPickerScreen(
    onMailboxSelected: (mailboxId: String, mailboxName: String) -> Unit,
    onOpenSettings: () -> Unit,
    onAllInboxesSelected: (domain: String) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: MailboxPickerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                mailboxName = mailbox.displayName,
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
                    title = { AppBarTitle("Mailboxes") },
                    actions = {
                        // Sign out lives in app Settings, a deliberate step further away than a
                        // top-bar icon, since it discards the stored credentials.
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    MailboxPickerContent(
                        state = state,
                        widthClass = widthClass,
                        onRefresh = viewModel::refresh,
                        onMailboxSelected = onMailboxSelected,
                        onAllInboxesSelected = onAllInboxesSelected,
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
                            ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MailboxPickerContent(
    state: MailboxPickerUiState,
    widthClass: WindowWidthClass,
    onRefresh: () -> Unit,
    onMailboxSelected: (mailboxId: String, mailboxName: String) -> Unit,
    onAllInboxesSelected: (domain: String) -> Unit,
    onDeleteRequested: (Mailbox) -> Unit,
    onCreateRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        // Skeleton cards on the same grid as real mailboxes — the screen looks like itself
        // while loading instead of showing a bare spinner.
        state.loading && state.mailboxes.isEmpty() -> Column(modifier = modifier) {
            repeat(3) {
                SkeletonEmailRow()
                HorizontalDivider(
                    modifier = Modifier.padding(start = ListRowDimens.textEdge),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }

        state.errorMessage != null && state.mailboxes.isEmpty() -> StatusPane(
            icon = Icons.Outlined.ErrorOutline,
            title = "Couldn't load mailboxes",
            detail = state.errorMessage,
            modifier = modifier,
        ) {
            Button(onClick = onRefresh) { Text("Retry") }
        }

        // Inside the refresh container: a user whose mailboxes failed to sync silently
        // shouldn't find the pull gesture dead-ended on the empty screen.
        state.mailboxes.isEmpty() -> PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = onRefresh,
            modifier = modifier,
        ) {
            StatusPane(
                icon = Icons.Rounded.Inbox,
                title = "No mailboxes yet",
                detail = "Create one to start reading and sending mail through your server.",
            ) {
                Button(onClick = onCreateRequested) { Text("New mailbox") }
            }
        }

        // One grouped column at every width: sticky domain titles give the list structure, and
        // each mailbox is a row on the same 72dp grid as mail. On a wide window the column is
        // capped and centered by the caller.
        else -> PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = onRefresh,
            modifier = modifier,
        ) {
            LazyColumn(
                // Bottom room so the last row can scroll clear of the New mailbox button.
                contentPadding = PaddingValues(bottom = 88.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                groupMailboxesByDomain(state.mailboxes).forEach { (domain, mailboxesInDomain) ->
                    stickyHeader(key = domain) {
                        DomainHeader(domain)
                    }
                    if (offersAllInboxes(mailboxesInDomain)) {
                        item(key = "all-$domain") {
                            AllInboxesRow(
                                domain = domain,
                                mailboxCount = mailboxesInDomain.size,
                                onClick = { onAllInboxesSelected(domain) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    items(mailboxesInDomain, key = { it.id }) { mailbox ->
                        MailboxRow(
                            mailbox = mailbox,
                            onClick = { onMailboxSelected(mailbox.id, mailbox.displayName) },
                            onDelete = { onDeleteRequested(mailbox) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

/** Mailboxes grouped by email domain, sorted alphabetically by domain — the grouping the
 * sticky-header list on phones renders. Domains are compared case-insensitively since email
 * domains aren't case-sensitive. */
internal fun groupMailboxesByDomain(mailboxes: List<Mailbox>): List<Pair<String, List<Mailbox>>> =
    mailboxes
        .groupBy { it.email.substringAfter('@').lowercase() }
        .toList()
        .sortedBy { (domain, _) -> domain }

/** Whether a domain's mailboxes get an "All inboxes" entry: with a single mailbox it would only
 * repeat that mailbox's own inbox. */
internal fun offersAllInboxes(mailboxesInDomain: List<Mailbox>): Boolean = mailboxesInDomain.size >= 2

/** The merged inbox of a whole domain, first in its group, on the same 72dp grid as a mailbox. */
@Composable
private fun AllInboxesRow(
    domain: String,
    mailboxCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "Open all inboxes for $domain", onClick = onClick)
                .padding(horizontal = ListRowDimens.horizontalPadding, vertical = ListRowDimens.verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(ListRowDimens.contentGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(ListRowDimens.avatarSize)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Inbox,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ListRowDimens.lineGap),
            ) {
                Text("All inboxes", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$mailboxCount mailboxes on $domain",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = ListRowDimens.textEdge),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

/** A domain's group title, on the 16dp edge. Opaque: a sticky header needs a solid background or
 * the rows scrolling underneath it show through. */
@Composable
private fun DomainHeader(domain: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = domain,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
}

/** One mailbox on the shared 72dp list grid: avatar in the 40dp slot, text from the 72dp edge,
 * and a divider inset to match. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MailboxRow(
    mailbox: Mailbox,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { showMenu = true })
                .padding(horizontal = ListRowDimens.horizontalPadding, vertical = ListRowDimens.verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(ListRowDimens.contentGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InitialsAvatar(name = mailbox.displayName, modifier = Modifier.size(ListRowDimens.avatarSize))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ListRowDimens.lineGap),
            ) {
                Text(mailbox.displayName, style = MaterialTheme.typography.titleMedium)
                // A mailbox with no display name falls back to its address — repeating the
                // same string on both lines reads as a rendering bug.
                if (mailbox.displayName != mailbox.email) {
                    Text(
                        mailbox.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Mailbox options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    // Reads as destructive before the confirmation dialog, not only in it.
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = ListRowDimens.textEdge),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
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
    // The address is the field users came to fill — focus it as soon as the form appears.
    val addressFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // Inside the ModalBottomSheet container (phones), this composes before the sheet's
        // entrance animation has attached the field's node to the tree, so an immediate
        // requestFocus() throws IllegalStateException — crashing the screen on every phone
        // "New mailbox" tap. Retry across a few frames instead of assuming attachment is instant.
        repeat(10) {
            try {
                addressFocusRequester.requestFocus()
                return@LaunchedEffect
            } catch (e: IllegalStateException) {
                withFrameNanos { }
            }
        }
    }

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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.weight(1f).focusRequester(addressFocusRequester),
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().focusRequester(addressFocusRequester),
            )
        }
        if (errorMessage != null) {
            ErrorBanner(errorMessage)
        }
    }
}
