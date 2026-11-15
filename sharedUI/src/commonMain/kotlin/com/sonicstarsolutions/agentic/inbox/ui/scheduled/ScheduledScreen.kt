package com.sonicstarsolutions.agentic.inbox.ui.scheduled

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduledEmail
import com.sonicstarsolutions.agentic.inbox.domain.model.ScheduledStatus
import com.sonicstarsolutions.agentic.inbox.theme.ListRowDimens
import com.sonicstarsolutions.agentic.inbox.ui.components.StatusPane
import com.sonicstarsolutions.agentic.inbox.util.ScheduleTimeFormatter
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Messages queued with "Send later", on the shared 72dp list grid. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledScreen(
    mailboxId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduledViewModel = koinViewModel { parametersOf(mailboxId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Live updates run only while the screen is visible: the socket opens on start, closes on stop.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.keepLive() }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var toCancel by remember { mutableStateOf<ScheduledEmail?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    toCancel?.let { item ->
        AlertDialog(
            onDismissRequest = { toCancel = null },
            title = { Text("Cancel scheduled message") },
            text = { Text("It won't be sent, and its text can't be recovered.") },
            confirmButton = {
                TextButton(onClick = { toCancel = null; viewModel.cancel(item) }) {
                    Text("Cancel message", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { toCancel = null }) { Text("Keep") } },
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Scheduled") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(padding))

            state.loadError != null -> StatusPane(
                icon = Icons.Default.ErrorOutline,
                title = "Couldn't load scheduled messages",
                detail = state.loadError,
                modifier = Modifier.padding(padding),
                action = { TextButton(onClick = viewModel::retry) { Text("Retry") } },
            )

            state.items.isEmpty() -> StatusPane(
                icon = Icons.Default.Schedule,
                title = "Nothing scheduled",
                detail = "Choose “Send later” in the composer to queue a message.",
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(state.items, key = { it.id }) { item ->
                    ScheduledRow(item = item, onCancel = { toCancel = item })
                    HorizontalDivider(modifier = Modifier.padding(start = ListRowDimens.textEdge))
                }
            }
        }
    }
}

@Composable
private fun ScheduledRow(item: ScheduledEmail, onCancel: () -> Unit) {
    val failed = item.status == ScheduledStatus.FAILED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ListRowDimens.horizontalPadding, vertical = ListRowDimens.verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(ListRowDimens.contentGap),
        verticalAlignment = Alignment.Top,
    ) {
        // Error container for a failed send, otherwise the neutral container: failure is the one
        // state worth a colour here.
        Surface(
            shape = CircleShape,
            color = if (failed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(ListRowDimens.avatarSize),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (failed) Icons.Default.ErrorOutline else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (failed) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ListRowDimens.lineGap)) {
            Text(
                text = item.to.ifBlank { "(no recipient)" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subject.ifBlank { "(no subject)" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = statusLine(item),
                style = MaterialTheme.typography.bodySmall,
                color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (failed && !item.error.isNullOrBlank()) {
                Text(
                    text = item.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // A message that is mid-send can't be cancelled, so it gets no button.
        if (item.status != ScheduledStatus.SENDING) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel scheduled message", modifier = Modifier.size(20.dp))
            }
        }
    }
}

private fun statusLine(item: ScheduledEmail): String {
    val time = ScheduleTimeFormatter.format(item.sendAt)
    return when (item.status) {
        ScheduledStatus.PENDING -> "Sends $time"
        ScheduledStatus.SENDING -> "Sending now…"
        ScheduledStatus.FAILED -> "Failed · was due $time"
    }
}
