package com.sonicstarsolutions.agentic.inbox.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicstarsolutions.agentic.inbox.domain.model.Contact
import com.sonicstarsolutions.agentic.inbox.ui.components.ErrorBanner
import com.sonicstarsolutions.agentic.inbox.ui.components.InitialsAvatar
import com.sonicstarsolutions.agentic.inbox.util.formatFileSize
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    mailboxId: String,
    mode: ComposeMode,
    emailId: String?,
    threadId: String?,
    draftId: String? = null,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {},
    onCancel: () -> Unit = {},
    viewModel: ComposeViewModel = koinViewModel { parametersOf(mailboxId, mode, emailId, threadId, draftId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCcBcc by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showSendLater by remember { mutableStateOf(false) }
    if (showSendLater) {
        SendLaterDialog(
            onDismiss = { showSendLater = false },
            onPick = { at ->
                showSendLater = false
                viewModel.scheduleSend(at)
            },
        )
    }

    // Reveal Cc/Bcc automatically if Reply All prefilled either of them — hiding fields that
    // already have recipients in them would be confusing, not tidy.
    LaunchedEffect(state.loading) {
        if (!state.loading && (state.cc.isNotBlank() || state.bcc.isNotBlank())) {
            showCcBcc = true
        }
    }

    LaunchedEffect(state.sent) {
        if (state.sent) onDone()
    }

    LaunchedEffect(state.discarded) {
        if (state.discarded) onCancel()
    }

    // Closing the composer keeps what was written. DisposableEffect rather than a callback on the
    // Close button, so backing out — gesture, hardware key, or button — all save alike.
    DisposableEffect(Unit) {
        onDispose { viewModel.saveDraftNow() }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard draft") },
            text = { Text("This draft will be deleted. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardDraft()
                    },
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep") }
            },
        )
    }

    // Errors render as a persistent inline banner above the fields (the app's form-error
    // convention) rather than a snackbar that vanishes while the user is still reading it.
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(titleFor(mode)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    val pickFiles = rememberAttachmentPicker(onFilePicked = viewModel::addAttachment)
                    IconButton(onClick = pickFiles, enabled = !state.sending) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach file")
                    }
                    IconButton(onClick = viewModel::send, enabled = !state.sending) {
                        if (state.sending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                    // Two icons plus an overflow: the design system caps an app bar at three.
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                                text = { Text("Send later…") },
                                enabled = !state.sending,
                                onClick = { showMenu = false; showSendLater = true },
                            )
                            // Only offered once there's actually a draft to throw away.
                            if (state.draftId != null) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                                    text = { Text("Discard draft") },
                                    enabled = !state.sending,
                                    onClick = { showMenu = false; showDiscardDialog = true },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        },
    ) { paddingValues ->
        if (state.loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        } else {
            val focusManager = LocalFocusManager.current
            val toFocusRequester = remember { FocusRequester() }
            // A brand-new message starts at the To field; replies and drafts already have
            // recipients, so the user's next act there is reading or writing, not addressing.
            LaunchedEffect(Unit) {
                if (mode == ComposeMode.NEW) toFocusRequester.requestFocus()
            }

            val addressKeyboard = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            )
            val nextField = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })

            // Capped and centered on wide windows — full-bleed single-line fields on a tablet
            // read as a stretched phone layout, not a composer.
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.TopCenter,
            ) {
            // Borderless rows split by hairline dividers — the mail-composer convention.
            // Outlined boxes around every field read as a settings form, not a message.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = FORM_MAX_WIDTH),
            ) {
                state.errorMessage?.let { message ->
                    ErrorBanner(message, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }

                if (state.fromAddress.isNotBlank()) {
                    FromRow(
                        fromAddress = state.fromAddress,
                        options = state.fromOptions,
                        selected = state.selectedFrom,
                        enabled = !state.sending,
                        onSelected = viewModel::onFromSelected,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                ComposeFieldRow(
                    value = state.to,
                    onValueChange = viewModel::onToChanged,
                    prefixLabel = "To",
                    keyboardOptions = addressKeyboard,
                    keyboardActions = nextField,
                    trailing = if (!showCcBcc) {
                        {
                            TextButton(onClick = { showCcBcc = true }) { Text("Cc/Bcc") }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.focusRequester(toFocusRequester),
                    onFocusLost = viewModel::dismissSuggestions,
                )
                SuggestionList(state.suggestions.takeIf { state.suggestionsFor == ComposeField.TO }.orEmpty(), viewModel::onSuggestionPicked)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (showCcBcc) {
                    ComposeFieldRow(
                        value = state.cc,
                        onValueChange = viewModel::onCcChanged,
                        prefixLabel = "Cc",
                        keyboardOptions = addressKeyboard,
                        keyboardActions = nextField,
                        onFocusLost = viewModel::dismissSuggestions,
                    )
                    SuggestionList(state.suggestions.takeIf { state.suggestionsFor == ComposeField.CC }.orEmpty(), viewModel::onSuggestionPicked)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ComposeFieldRow(
                        value = state.bcc,
                        onValueChange = viewModel::onBccChanged,
                        prefixLabel = "Bcc",
                        keyboardOptions = addressKeyboard,
                        keyboardActions = nextField,
                        onFocusLost = viewModel::dismissSuggestions,
                    )
                    SuggestionList(state.suggestions.takeIf { state.suggestionsFor == ComposeField.BCC }.orEmpty(), viewModel::onSuggestionPicked)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                ComposeFieldRow(
                    value = state.subject,
                    onValueChange = viewModel::onSubjectChanged,
                    placeholder = "Subject",
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = nextField,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (state.attachments.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        itemsIndexed(state.attachments) { index, attachment ->
                            InputChip(
                                selected = false,
                                onClick = { viewModel.removeAttachment(index) },
                                label = { Text("${attachment.filename} · ${formatFileSize(attachment.bytes.size.toLong())}") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove attachment",
                                        modifier = Modifier.size(InputChipDefaults.IconSize),
                                    )
                                },
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }

                TextField(
                    value = state.body,
                    onValueChange = viewModel::onBodyChanged,
                    placeholder = { Text("Compose email") },
                    colors = borderlessFieldColors(),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
            }
        }
    }
}

/** One borderless single-line field row: an optional muted prefix label ("To"), the input, and
 * an optional trailing control — the shape every recipient/subject row shares. */
@Composable
private fun ComposeFieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefixLabel: String? = null,
    placeholder: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailing: (@Composable () -> Unit)? = null,
    /** Called when the field loses focus — lets recipient fields close their suggestion list. */
    onFocusLost: (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        prefix = prefixLabel?.let {
            {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        },
        placeholder = placeholder?.let { { Text(it) } },
        trailingIcon = trailing,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = borderlessFieldColors(),
        modifier = modifier
            .fillMaxWidth()
            .let { base ->
                if (onFocusLost == null) base else base.onFocusChanged { focus -> if (!focus.isFocused) onFocusLost() }
            },
    )
}

@Composable
private fun borderlessFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)

/** Fields stay readable up to roughly this width; past it a composer is just stretched. */
private val FORM_MAX_WIDTH = 720.dp

private fun titleFor(mode: ComposeMode): String = when (mode) {
    ComposeMode.NEW -> "New message"
    ComposeMode.REPLY -> "Reply"
    ComposeMode.REPLY_ALL -> "Reply all"
    ComposeMode.FORWARD -> "Forward"
    ComposeMode.EDIT_DRAFT -> "Edit draft"
}

/**
 * The read-only "From" row — or, when the mailbox has aliases, a row that opens a menu of the
 * addresses the message can go out from. The row is the whole tap target (48dp+), so the small
 * chevron never has to be hit on its own.
 */
@Composable
private fun FromRow(
    fromAddress: String,
    options: List<String>,
    selected: String,
    enabled: Boolean,
    onSelected: (String) -> Unit,
) {
    val canChoose = options.size > 1
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (canChoose && enabled) Modifier.clickable { expanded = true } else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "From",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = fromAddress,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (canChoose) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Choose sender address",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { address ->
                DropdownMenuItem(
                    text = { Text(address) },
                    trailingIcon = {
                        if (address.equals(selected, ignoreCase = true)) {
                            Icon(Icons.Default.Check, contentDescription = "Selected")
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelected(address)
                    },
                )
            }
        }
    }
}

/**
 * Contacts matching the address being typed, listed directly under its field. Each row is the
 * whole tap target (48dp+) and reuses the inbox's avatar so identities look the same everywhere.
 */
@Composable
private fun SuggestionList(contacts: List<Contact>, onPick: (Contact) -> Unit) {
    if (contacts.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        contacts.forEach { contact ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onPick(contact) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                InitialsAvatar(name = contact.address, modifier = Modifier.size(32.dp))
                Text(
                    text = contact.address,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
