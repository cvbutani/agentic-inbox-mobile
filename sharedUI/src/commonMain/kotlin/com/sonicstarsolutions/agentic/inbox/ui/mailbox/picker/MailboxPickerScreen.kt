package com.sonicstarsolutions.agentic.inbox.ui.mailbox.picker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicstarsolutions.agentic.inbox.domain.model.Mailbox
import com.sonicstarsolutions.agentic.inbox.ui.components.SectionTitle
import org.koin.compose.viewmodel.koinViewModel

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

    var showCreateDialog by remember { mutableStateOf(false) }
    LaunchedEffect(state.mailboxCreated) {
        if (state.mailboxCreated) {
            showCreateDialog = false
            viewModel.consumeMailboxCreated()
        }
    }

    if (showCreateDialog) {
        CreateMailboxDialog(
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
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New mailbox")
            }
        },
    ) { scaffoldPadding ->
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(contentPadding)
                .padding(scaffoldPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Your mailboxes")
            TextButton(onClick = viewModel::signOut) { Text("Sign out") }

            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.errorMessage != null -> Text(
                    state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                )

                state.mailboxes.isEmpty() -> Text("No mailboxes yet.")

                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(state.mailboxes, key = { it.id }) { mailbox ->
                        MailboxCard(
                            mailbox = mailbox,
                            onClick = { onMailboxSelected(mailbox.id, mailbox.name) },
                            onDelete = { mailboxToDelete = mailbox },
                        )
                    }
                }
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

    Box {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { showMenu = true }),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    mailbox.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    mailbox.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

@Composable
private fun CreateMailboxDialog(
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

    AlertDialog(
        onDismissRequest = { if (!creating) onDismiss() },
        title = { Text("New mailbox") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
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
                            onValueChange = { localPart = it },
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
                                TextButton(onClick = { if (!creating) domainMenuExpanded = true }) {
                                    Text(selectedDomain)
                                }
                                DropdownMenu(
                                    expanded = domainMenuExpanded,
                                    onDismissRequest = { domainMenuExpanded = false },
                                ) {
                                    allowedDomains.forEach { domain ->
                                        DropdownMenuItem(
                                            text = { Text(domain) },
                                            onClick = {
                                                selectedDomain = domain
                                                domainMenuExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // No domains loaded (still loading, or the Worker's config genuinely has
                    // none) — fall back to a plain free-text address so the flow isn't a dead end.
                    OutlinedTextField(
                        value = freeformEmail,
                        onValueChange = { freeformEmail = it },
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
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(email, name) },
                enabled = !creating && canSubmit,
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
