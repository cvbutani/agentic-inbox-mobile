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
import androidx.compose.runtime.LaunchedEffect
import com.sonicstarsolutions.agentic.inbox.domain.model.Draft
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.ui.WindowWidthClass
import com.sonicstarsolutions.agentic.inbox.ui.thread.ThreadScreen
import com.sonicstarsolutions.agentic.inbox.ui.windowWidthClassFor
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf


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
    /** Non-null for the merged "All inboxes" view of a domain. */
    domain: String? = null,
    onSwitchMailbox: () -> Unit = {},
    onEmailSelected: (EmailSummary) -> Unit = {},
    onDraftSelected: (Draft) -> Unit = {},
    onComposeNew: () -> Unit = {},
    onSearch: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenScheduled: () -> Unit = {},
    onReply: (mailboxId: String, emailId: String, threadId: String?) -> Unit = { _, _, _ -> },
    onReplyAll: (mailboxId: String, emailId: String, threadId: String?) -> Unit = { _, _, _ -> },
    onForward: (mailboxId: String, emailId: String, threadId: String?) -> Unit = { _, _, _ -> },
    onEditDraft: (mailboxId: String, draftMessageId: String, threadId: String?) -> Unit = { _, _, _ -> },
    /** Fired when the window narrows out of the split while an email is open in the detail
     * pane — the host should push it as a full-screen thread so it doesn't silently vanish. */
    onOpenEmailFullScreen: (mailboxId: String, emailId: String, threadId: String?) -> Unit = { _, _, _ -> },
    /** An address picked in the detail pane's From line: search the open mailbox for that sender. */
    onSearchFrom: (mailboxId: String, address: String) -> Unit = { _, _ -> },
    /** An address picked from To, Cc or Reply-To: search the open mailbox for it. */
    onSearchFor: (mailboxId: String, text: String) -> Unit = { _, _ -> },
    viewModel: InboxViewModel = koinViewModel { parametersOf(mailboxId, mailboxName, domain) },
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Below EXPANDED a split would leave both panes too narrow to read, so phones and
        // tablets in portrait stay single-pane. Shared breakpoint, not a local constant, so
        // "tablet" means the same width here as on every other adaptive screen.
        val twoPane = windowWidthClassFor(maxWidth) == WindowWidthClass.EXPANDED

        var openEmailId by rememberSaveable { mutableStateOf<String?>(null) }
        var openThreadId by rememberSaveable { mutableStateOf<String?>(null) }
        // In the domain-wide view the open email can be in any of the domain's mailboxes.
        var openMailboxId by rememberSaveable { mutableStateOf<String?>(null) }

        // Rotating out of the split with a conversation open used to just hide it; hand it to
        // the back stack instead so the user keeps what they were reading.
        LaunchedEffect(twoPane) {
            if (!twoPane) {
                val emailId = openEmailId ?: return@LaunchedEffect
                val threadId = openThreadId
                val openMailbox = openMailboxId ?: mailboxId
                openEmailId = null
                openThreadId = null
                openMailboxId = null
                onOpenEmailFullScreen(openMailbox, emailId, threadId)
            }
        }

        if (!twoPane) {
            InboxScreen(
                mailboxId = mailboxId,
                mailboxName = mailboxName,
                onSwitchMailbox = onSwitchMailbox,
                onEmailSelected = onEmailSelected,
                onDraftSelected = onDraftSelected,
                onComposeNew = onComposeNew,
                onSearch = onSearch,
                onOpenSettings = onOpenSettings,
                onOpenScheduled = onOpenScheduled,
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
                    openMailboxId = email.mailboxId.ifBlank { mailboxId }
                },
                onDraftSelected = onDraftSelected,
                onComposeNew = onComposeNew,
                onSearch = onSearch,
                onOpenSettings = onOpenSettings,
                onOpenScheduled = onOpenScheduled,
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
                        val threadMailboxId = openMailboxId ?: mailboxId
                        ThreadScreen(
                            mailboxId = threadMailboxId,
                            emailId = emailId,
                            threadId = openThreadId,
                            useCloseAffordance = true,
                            onBack = {
                                // In this layout there's nowhere to go "back" to — the list is
                                // already onscreen. Closing the thread means emptying the pane,
                                // and refreshing so an archive/delete leaves the list too.
                                openEmailId = null
                                openThreadId = null
                                openMailboxId = null
                                viewModel.onRefresh()
                            },
                            onReply = { id -> onReply(threadMailboxId, id, openThreadId) },
                            onReplyAll = { id -> onReplyAll(threadMailboxId, id, openThreadId) },
                            onForward = { id -> onForward(threadMailboxId, id, openThreadId) },
                            onEditDraft = { id -> onEditDraft(threadMailboxId, id, openThreadId) },
                            onSearchFrom = { address -> onSearchFrom(threadMailboxId, address) },
                            onSearchFor = { text -> onSearchFor(threadMailboxId, text) },
                            viewModel = koinViewModel(key = "thread-$threadMailboxId-$emailId") {
                                parametersOf(threadMailboxId, emailId, openThreadId)
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
                    tint = MaterialTheme.colorScheme.outlineVariant,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
