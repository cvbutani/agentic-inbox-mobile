package com.sonicstarsolutions.agentic.inbox.ui.mailbox.agentaccess

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicstarsolutions.agentic.inbox.domain.model.AgentKey
import com.sonicstarsolutions.agentic.inbox.domain.model.AgentScope
import com.sonicstarsolutions.agentic.inbox.ui.components.StatusPane
import com.sonicstarsolutions.agentic.inbox.util.EmailTimeFormatter
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentAccessScreen(
    mailboxId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    viewModel: AgentAccessViewModel = koinViewModel { parametersOf(mailboxId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var keyToRevoke by remember { mutableStateOf<AgentKey?>(null) }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    // The create dialog closes once its request is accepted (a secret appears) — not before, so
    // a refused or failed create leaves what the user typed in place.
    LaunchedEffect(state.createdKey) {
        if (state.createdKey != null) showCreateDialog = false
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Agent access") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!state.loading && state.loadError == null) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New key") },
                )
            }
        },
    ) { padding ->
        when {
            state.loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(padding))

            state.loadError != null -> StatusPane(
                icon = Icons.Default.ErrorOutline,
                title = "Couldn't load agent keys",
                detail = state.loadError,
                modifier = Modifier.padding(padding),
                action = { TextButton(onClick = viewModel::retry) { Text("Retry") } },
            )

            state.keys.isEmpty() -> StatusPane(
                icon = Icons.Default.VpnKey,
                title = "No agent keys",
                detail = "Create a key to let an automated agent read, draft or send mail for this mailbox.",
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(modifier = Modifier.padding(padding)) {
                items(state.keys, key = { it.id }) { key ->
                    AgentKeyRow(key = key, onRevoke = { keyToRevoke = key })
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                }
                // Keeps the last row clear of the floating action button.
                item { Box(modifier = Modifier.size(88.dp)) }
            }
        }
    }

    if (showCreateDialog) {
        CreateKeyDialog(
            saving = state.saving,
            onCreate = viewModel::create,
            onDismiss = { showCreateDialog = false },
        )
    }

    state.createdKey?.let { created ->
        SecretDialog(keyName = created.key.name, secret = created.secret, onDone = viewModel::dismissSecret)
    }

    keyToRevoke?.let { key ->
        AlertDialog(
            onDismissRequest = { keyToRevoke = null },
            title = { Text("Revoke “${key.name}”?") },
            text = { Text("Anything using this key stops working immediately. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.revoke(key.id); keyToRevoke = null }) { Text("Revoke") }
            },
            dismissButton = { TextButton(onClick = { keyToRevoke = null }) { Text("Cancel") } },
        )
    }
}

/** A list row on the shared 72dp text edge: 40dp leading slot + 16dp gutter inside 16dp padding. */
@Composable
private fun AgentKeyRow(key: AgentKey, onRevoke: () -> Unit) {
    val titleColor = if (key.revoked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = null,
                tint = if (key.revoked) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = key.name, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            Text(
                text = agentKeySummary(key),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = buildString {
                    append(key.prefix)
                    append(" · ")
                    append(if (key.revoked) "Revoked" else "Created ${EmailTimeFormatter.formatAbsolute(key.createdAt)}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!key.revoked) {
            TextButton(onClick = onRevoke) { Text("Revoke") }
        }
    }
}

@Composable
private fun CreateKeyDialog(
    saving: Boolean,
    onCreate: (name: String, scopes: Set<AgentScope>, dailySendLimit: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(setOf(AgentScope.READ.name)) }
    var limit by rememberSaveable { mutableStateOf("") }
    val scopes = selected.map { AgentScope.valueOf(it) }.toSet()

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("New agent key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    supportingText = { Text("So you can recognise it later, e.g. “Claude Code”") },
                    singleLine = true,
                )
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AgentScope.entries.forEach { scope ->
                    val checked = scope in scopes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = { on ->
                                    selected = if (on) selected + scope.name else selected - scope.name
                                },
                            )
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // The Row owns the click; the Checkbox is just its visual.
                        Checkbox(checked = checked, onCheckedChange = null)
                        Column {
                            Text(text = scope.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = scope.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (AgentScope.SEND in scopes) {
                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it.filter(Char::isDigit).take(4) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Daily send limit") },
                        supportingText = { Text("Sends per day. Leave blank for the default of 20.") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name, scopes, limit.toIntOrNull()) },
                enabled = !saving && name.isNotBlank() && scopes.isNotEmpty(),
            ) { Text(if (saving) "Creating…" else "Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } },
    )
}

/** The only place a secret is ever shown. It can't be dismissed by a stray tap or back press:
 * once it's gone it can't be recovered, only replaced with a new key. */
@Composable
private fun SecretDialog(keyName: String, secret: String, onDone: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text("Copy your key now") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "This is the only time the key for “$keyName” is shown. Store it somewhere safe; " +
                        "if you lose it, revoke it and create a new one.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                SelectionContainer {
                    Text(text = secret, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDone) { Text("Done") } },
        dismissButton = {
            TextButton(onClick = { clipboard.setText(AnnotatedString(secret)); copied = true }) {
                Text(if (copied) "Copied" else "Copy")
            }
        },
    )
}
