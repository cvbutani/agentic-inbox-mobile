package com.sonicstarsolutions.agentic.inbox.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val snackbarHostState = remember { SnackbarHostState() }
    var showCcBcc by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeError()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(titleFor(mode)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    // Only offered once there's actually a draft to throw away.
                    if (state.draftId != null) {
                        IconButton(onClick = { showDiscardDialog = true }, enabled = !state.sending) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Discard draft")
                        }
                    }
                    IconButton(onClick = viewModel::send, enabled = !state.sending) {
                        if (state.sending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OutlinedTextField(
                    value = state.to,
                    onValueChange = viewModel::onToChanged,
                    label = { Text("To") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showCcBcc) {
                    OutlinedTextField(
                        value = state.cc,
                        onValueChange = viewModel::onCcChanged,
                        label = { Text("Cc") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.bcc,
                        onValueChange = viewModel::onBccChanged,
                        label = { Text("Bcc") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = "Cc/Bcc",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.End)
                            .clickable { showCcBcc = true }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                    )
                }
                OutlinedTextField(
                    value = state.subject,
                    onValueChange = viewModel::onSubjectChanged,
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                OutlinedTextField(
                    value = state.body,
                    onValueChange = viewModel::onBodyChanged,
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}

private fun titleFor(mode: ComposeMode): String = when (mode) {
    ComposeMode.NEW -> "New message"
    ComposeMode.REPLY -> "Reply"
    ComposeMode.REPLY_ALL -> "Reply all"
    ComposeMode.FORWARD -> "Forward"
    ComposeMode.EDIT_DRAFT -> "Edit draft"
}
