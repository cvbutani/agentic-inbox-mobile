package com.sonicstarsolutions.agentic.inbox.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sonicstarsolutions.agentic.inbox.ui.compose.ComposeMode
import com.sonicstarsolutions.agentic.inbox.ui.compose.ComposeScreen
import com.sonicstarsolutions.agentic.inbox.ui.inbox.InboxListDetailScreen
import com.sonicstarsolutions.agentic.inbox.ui.mailbox.picker.MailboxPickerScreen
import com.sonicstarsolutions.agentic.inbox.ui.onboarding.OnboardingScreen
import com.sonicstarsolutions.agentic.inbox.ui.search.SearchScreen
import com.sonicstarsolutions.agentic.inbox.ui.splash.SplashScreen
import com.sonicstarsolutions.agentic.inbox.ui.thread.ThreadScreen

/**
 * Root Navigation 3 host.
 *
 * Every transition is an explicit callback fired by the screen that decided it — [Splash] decides
 * the one-time startup destination, [OnboardingScreen] decides when credentials are saved, and
 * [MailboxPickerScreen] decides on sign-out. This host has no reactive routing logic of its own;
 * it just wires those callbacks to a [Navigator].
 */
@Composable
fun AppNavHost() {
    val backStack = rememberNavBackStack(appNavConfiguration, Splash)
    val navigator = remember(backStack) { Navigator(backStack) }

    NavDisplay(
        backStack = navigator.backStack,
        onBack = { navigator.goBack() },
        entryDecorators = listOf(
            // Retains composable-saveable state (scroll positions, rememberSaveable) per entry.
            rememberSaveableStateHolderNavEntryDecorator(),
            // Scopes ViewModels (incl. Koin's koinViewModel) to the entry and clears them on pop.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Splash> {
                SplashScreen(
                    onSignedIn = { navigator.replaceRoot(MailboxPicker) },
                    onSignedOut = { navigator.replaceRoot(Onboarding) },
                )
            }

            entry<Onboarding> {
                OnboardingScreen(onSaved = { navigator.replaceRoot(MailboxPicker) })
            }

            entry<MailboxPicker> {
                MailboxPickerScreen(
                    onMailboxSelected = { mailboxId, mailboxName ->
                        navigator.goTo(Inbox(mailboxId, mailboxName))
                    },
                    onSignedOut = { navigator.replaceRoot(Onboarding) },
                )
            }

            entry<Inbox> { key ->
                InboxListDetailScreen(
                    mailboxId = key.mailboxId,
                    mailboxName = key.mailboxName,
                    onSwitchMailbox = { navigator.goTo(MailboxPicker) },
                    // Only fires in the single-pane layout; on a wide window the thread opens in
                    // the detail pane and never reaches the back stack.
                    onEmailSelected = { email ->
                        navigator.goTo(EmailThread(key.mailboxId, email.id, email.threadId))
                    },
                    onReply = { emailId, threadId ->
                        navigator.goTo(Compose(key.mailboxId, ComposeMode.REPLY.name, emailId, threadId))
                    },
                    onReplyAll = { emailId, threadId ->
                        navigator.goTo(Compose(key.mailboxId, ComposeMode.REPLY_ALL.name, emailId, threadId))
                    },
                    onForward = { emailId, threadId ->
                        navigator.goTo(Compose(key.mailboxId, ComposeMode.FORWARD.name, emailId, threadId))
                    },
                    onEditDraft = { draftMessageId, threadId ->
                        navigator.goTo(Compose(key.mailboxId, ComposeMode.EDIT_DRAFT.name, draftMessageId, threadId))
                    },
                    onDraftSelected = { draft ->
                        // A resumed draft reopens in the mode it was written in, so a half-written
                        // reply still replies to its original message rather than becoming a new one.
                        navigator.goTo(
                            Compose(
                                mailboxId = key.mailboxId,
                                mode = draft.mode,
                                emailId = draft.originalEmailId,
                                threadId = draft.threadId,
                                draftId = draft.id,
                            ),
                        )
                    },
                    onComposeNew = {
                        navigator.goTo(Compose(mailboxId = key.mailboxId, mode = ComposeMode.NEW.name))
                    },
                    onSearch = {
                        navigator.goTo(Search(mailboxId = key.mailboxId))
                    },
                )
            }

            entry<Search> { key ->
                SearchScreen(
                    mailboxId = key.mailboxId,
                    onBack = { navigator.goBack() },
                    onEmailSelected = { email ->
                        navigator.goTo(EmailThread(key.mailboxId, email.id, email.threadId))
                    },
                )
            }

            entry<EmailThread> { key ->
                ThreadScreen(
                    mailboxId = key.mailboxId,
                    emailId = key.emailId,
                    threadId = key.threadId,
                    onBack = { navigator.goBack() },
                    onReply = { originalEmailId ->
                        navigator.goTo(Compose(key.mailboxId, ComposeMode.REPLY.name, originalEmailId, key.threadId))
                    },
                    onReplyAll = { originalEmailId ->
                        navigator.goTo(Compose(key.mailboxId, ComposeMode.REPLY_ALL.name, originalEmailId, key.threadId))
                    },
                    onForward = { originalEmailId ->
                        navigator.goTo(Compose(key.mailboxId, ComposeMode.FORWARD.name, originalEmailId, key.threadId))
                    },
                    onEditDraft = { draftMessageId ->
                        navigator.goTo(Compose(key.mailboxId, ComposeMode.EDIT_DRAFT.name, draftMessageId, key.threadId))
                    },
                )
            }

            entry<Compose> { key ->
                ComposeScreen(
                    mailboxId = key.mailboxId,
                    mode = ComposeMode.valueOf(key.mode),
                    emailId = key.emailId,
                    threadId = key.threadId,
                    draftId = key.draftId,
                    onDone = { navigator.goBack() },
                    onCancel = { navigator.goBack() },
                )
            }
        },
    )
}
