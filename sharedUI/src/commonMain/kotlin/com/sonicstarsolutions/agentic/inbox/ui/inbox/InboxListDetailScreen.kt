package com.sonicstarsolutions.agentic.inbox.ui.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sonicstarsolutions.agentic.inbox.domain.model.Draft
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.ui.thread.ThreadScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Material 3's "expanded" width class. Below it, a split view would leave both panes too narrow
 * to read, so phones and tablets in portrait stay single-pane. */
private val TWO_PANE_MIN_WIDTH = 840.dp

/**
 * The Inbox destination: an email list that, on a wide enough window, keeps the conversation
 * beside it instead of navigating away to it.
 *
 * The two-pane branch deliberately doesn't touch the back stack — the thread is composed inline
 * rather than pushed — so the same [InboxViewModel] instance backs both branches and rotating a
 * tablet doesn't reload the list. [onEmailSelected] is only called in the single-pane branch,
 * where opening an email really is a navigation.
 */
@Composable
fun InboxListDetailScreen(
    mailboxId: String,
    mailboxName: String,
    modifier: Modifier = Modifier,
    onSwitchMailbox: () -> Unit = {},
    onEmailSelected: (EmailSummary) -> Unit = {},
    onDraftSelected: (Draft) -> Unit = {},
    onComposeNew: () -> Unit = {},
    onSearch: () -> Unit = {},
    onReply: (emailId: String, threadId: String?) -> Unit = { _, _ -> },
    onReplyAll: (emailId: String, threadId: String?) -> Unit = { _, _ -> },
    onForward: (emailId: String, threadId: String?) -> Unit = { _, _ -> },
    onEditDraft: (draftMessageId: String, threadId: String?) -> Unit = { _, _ -> },
    viewModel: InboxViewModel = koinViewModel { parametersOf(mailboxId, mailboxName) },
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val twoPane = maxWidth >= TWO_PANE_MIN_WIDTH

        var openEmailId by rememberSaveable { mutableStateOf<String?>(null) }
        var openThreadId by rememberSaveable { mutableStateOf<String?>(null) }

        if (!twoPane) {
            InboxScreen(
                mailboxId = mailboxId,
                mailboxName = mailboxName,
                onSwitchMailbox = onSwitchMailbox,
                onEmailSelected = onEmailSelected,
                onDraftSelected = onDraftSelected,
                onComposeNew = onComposeNew,
                onSearch = onSearch,
                viewModel = viewModel,
            )
            return@BoxWithConstraints
        }

        Row(modifier = Modifier.fillMaxSize()) {
            InboxScreen(
                mailboxId = mailboxId,
                mailboxName = mailboxName,
                modifier = Modifier.weight(LIST_PANE_WEIGHT),
                onSwitchMailbox = onSwitchMailbox,
                onEmailSelected = { email ->
                    openEmailId = email.id
                    openThreadId = email.threadId
                },
                onDraftSelected = onDraftSelected,
                onComposeNew = onComposeNew,
                onSearch = onSearch,
                openEmailId = openEmailId,
                viewModel = viewModel,
            )

            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(modifier = Modifier.weight(DETAIL_PANE_WEIGHT).fillMaxSize()) {
                val emailId = openEmailId
                if (emailId == null) {
                    NoMessageSelectedPane()
                } else {
                    // Keyed so selecting a different email builds a fresh composition, and the
                    // koinViewModel key so it gets that email's ThreadViewModel rather than
                    // reusing the previous one from this same ViewModelStoreOwner.
                    key(emailId) {
                        ThreadScreen(
                            mailboxId = mailboxId,
                            emailId = emailId,
                            threadId = openThreadId,
                            onBack = {
                                // In this layout there's nowhere to go "back" to — the list is
                                // already onscreen. Closing the thread means emptying the pane,
                                // and refreshing so an archive/delete leaves the list too.
                                openEmailId = null
                                openThreadId = null
                                viewModel.onRefresh()
                            },
                            onReply = { id -> onReply(id, openThreadId) },
                            onReplyAll = { id -> onReplyAll(id, openThreadId) },
                            onForward = { id -> onForward(id, openThreadId) },
                            onEditDraft = { id -> onEditDraft(id, openThreadId) },
                            viewModel = koinViewModel(key = "thread-$emailId") {
                                parametersOf(mailboxId, emailId, openThreadId)
                            },
                        )
                    }
                }
            }
        }
    }
}

private const val LIST_PANE_WEIGHT = 0.38f
private const val DETAIL_PANE_WEIGHT = 0.62f

@Composable
private fun NoMessageSelectedPane() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(56.dp),
                )
                Text(
                    text = "No message selected",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Pick a message from the list to read it here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
