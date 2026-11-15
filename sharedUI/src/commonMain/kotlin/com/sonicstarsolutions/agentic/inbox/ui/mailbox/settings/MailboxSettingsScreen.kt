package com.sonicstarsolutions.agentic.inbox.ui.mailbox.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicstarsolutions.agentic.inbox.ui.components.AppBarTitle
import com.sonicstarsolutions.agentic.inbox.ui.components.InitialsAvatar
import com.sonicstarsolutions.agentic.inbox.ui.components.StatusPane
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MailboxSettingsScreen(
    mailboxId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    viewModel: MailboxSettingsViewModel = koinViewModel { parametersOf(mailboxId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeMessage()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { AppBarTitle("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // The one primary action, offered only once there is something to save.
            AnimatedVisibility(visible = state.dirty, enter = fadeIn(), exit = fadeOut()) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::save,
                    icon = {
                        if (state.saving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                        }
                    },
                    text = { Text("Save") },
                )
            }
        },
    ) { padding ->
        when {
            state.loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(padding))

            state.loadError != null -> StatusPane(
                icon = Icons.Default.ErrorOutline,
                title = "Couldn't load settings",
                detail = state.loadError,
                modifier = Modifier.padding(padding),
                action = { TextButton(onClick = viewModel::retry) { Text("Retry") } },
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    // Keeps the last field clear of the floating Save button.
                    .padding(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SectionLabel("Display name")
                OutlinedTextField(
                    value = state.form.fromName,
                    onValueChange = viewModel::onFromNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name shown on sent mail") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )

                // One divider in the screen: between the profile (name, signature) and the aliases.
                Spacer(Modifier.height(8.dp))
                SectionLabel("Signature")
                SwitchRow(
                    label = "Add a signature to new messages",
                    checked = state.form.signatureEnabled,
                    onCheckedChange = viewModel::onSignatureEnabledChanged,
                )
                AnimatedVisibility(
                    visible = state.form.signatureEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    OutlinedTextField(
                        value = state.form.signatureText,
                        onValueChange = viewModel::onSignatureTextChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Signature") },
                        minLines = 3,
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                AliasesSection(
                    aliases = state.form.aliases,
                    error = state.aliasError,
                    onAdd = viewModel::onAliasAdded,
                    onRemove = viewModel::onAliasRemoved,
                    // True once the entry was accepted, so the field can empty itself.
                    wasAccepted = { viewModel.state.value.aliasError == null },
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

/** The whole row toggles, not just the switch — the touch target is the full 56dp+ row. */
@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supporting: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // The Row handles the click; the Switch is purely a visual of that state.
        Switch(checked = checked, onCheckedChange = null)
    }
}

/**
 * Extra addresses the mailbox answers to and can send from. Entries are added and removed in the
 * form like every other field; Save writes them. Each row's remove button keeps the 48dp floor.
 */
@Composable
private fun AliasesSection(
    aliases: List<String>,
    error: String?,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    wasAccepted: () -> Boolean,
) {
    var entry by rememberSaveable { mutableStateOf("") }
    val submit = {
        onAdd(entry)
        if (wasAccepted()) entry = ""
    }

    SectionLabel("Aliases")
    Text(
        text = "Other addresses on your domains that deliver to this mailbox. You can send from them too.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    aliases.forEach { alias ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InitialsAvatar(name = alias, modifier = Modifier.size(32.dp))
            Text(
                text = alias,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onRemove(alias) }) {
                Icon(Icons.Default.Close, contentDescription = "Remove $alias", modifier = Modifier.size(20.dp))
            }
        }
    }
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = entry,
            onValueChange = { entry = it },
            modifier = Modifier.weight(1f),
            label = { Text("Add an address") },
            singleLine = true,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { submit() }),
        )
        TextButton(onClick = submit, enabled = entry.isNotBlank(), modifier = Modifier.padding(top = 8.dp)) { Text("Add") }
    }
}
