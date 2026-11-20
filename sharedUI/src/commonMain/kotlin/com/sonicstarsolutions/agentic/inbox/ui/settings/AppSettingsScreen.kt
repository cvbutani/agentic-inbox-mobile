package com.sonicstarsolutions.agentic.inbox.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicstarsolutions.agentic.inbox.domain.model.AppLinks
import com.sonicstarsolutions.agentic.inbox.domain.model.BackendInfo
import com.sonicstarsolutions.agentic.inbox.ui.components.AppBarTitle
import com.sonicstarsolutions.agentic.inbox.ui.mailbox.settings.SectionLabel
import com.sonicstarsolutions.agentic.inbox.ui.mailbox.settings.SwipeSettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

/** Settings column stays readable on a wide window rather than stretching edge to edge. */
private val CONTENT_MAX_WIDTH = 560.dp

/**
 * App-level settings, reached from the mailbox picker: which backend the app needs and which
 * server it is connected to, device preferences, the app version, and sign out. Everything about
 * a particular mailbox stays in that mailbox's own settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppSettingsViewModel = koinViewModel(),
    swipeViewModel: SwipeSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val swipeSettings by swipeViewModel.settings.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(state.signedOut) {
        if (state.signedOut) {
            viewModel.consumeSignedOut()
            onSignedOut()
        }
    }

    // Signing out discards the stored server credentials, and the Access secret was shown only
    // once at creation: one mis-tap shouldn't be able to lock the user out.
    var showSignOutDialog by remember { mutableStateOf(false) }
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            title = { Text("Sign out") },
            text = { Text("You'll need your server URL and Access credentials to reconnect.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        viewModel.signOut()
                    },
                ) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            },
        )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = CONTENT_MAX_WIDTH)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BackendSection(
                    backendName = state.backendName,
                    connectedHost = state.connectedHost,
                    onOpenRepository = { uriHandler.openUri(state.repoUrl) },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SwipeActionsSection(
                    settings = swipeSettings,
                    onSwipeRightChanged = swipeViewModel::onSwipeRightChanged,
                    onSwipeLeftChanged = swipeViewModel::onSwipeLeftChanged,
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                AboutSection(
                    versionName = state.versionName,
                    onOpenPrivacy = { uriHandler.openUri(AppLinks.PRIVACY_URL) },
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SectionLabel("Account")
                TextButton(
                    onClick = { showSignOutDialog = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign out")
                }
            }
        }
    }
}

/**
 * Which backend the app needs, and which server this device is connected to. The app is built for
 * the Sonic Star fork of agentic-inbox; this is where a user who is unsure why something is
 * missing can check what they deployed. The server's host is shown, never its credentials.
 */
@Composable
private fun BackendSection(
    backendName: String,
    connectedHost: String?,
    onOpenRepository: () -> Unit,
) {
    SectionLabel("Backend")
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = backendName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = BackendInfo.REQUIREMENT,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ValueRow(label = "Connected to", value = connectedHost ?: "Not connected")
            TextButton(onClick = onOpenRepository) {
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(BackendInfo.REPO_LINK_LABEL)
            }
        }
    }
}

@Composable
private fun AboutSection(versionName: String, onOpenPrivacy: () -> Unit) {
    SectionLabel("About")
    ValueRow(label = "Version", value = versionName)
    TextButton(onClick = onOpenPrivacy) {
        Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Privacy policy")
    }
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
