package com.sonicstarsolutions.agentic.inbox.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.sonicstarsolutions.agentic.inbox.ui.inbox.InboxScreen
import com.sonicstarsolutions.agentic.inbox.ui.mailbox.picker.MailboxPickerScreen
import com.sonicstarsolutions.agentic.inbox.ui.onboarding.OnboardingScreen
import com.sonicstarsolutions.agentic.inbox.ui.splash.SplashScreen

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

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            // Retains composable-saveable state (scroll positions, rememberSaveable) per entry.
            rememberSaveableStateHolderNavEntryDecorator(),
            // Scopes ViewModels (incl. Koin's koinViewModel) to the entry and clears them on pop.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Splash> {
                SplashScreen(
                    onSignedIn = { backStack.add(MailboxPicker) },
                    onSignedOut = { backStack.add(Onboarding) },
                )
            }

            entry<Onboarding> {
                OnboardingScreen(onSaved = { backStack.add(MailboxPicker) })
            }

            entry<MailboxPicker> {
                MailboxPickerScreen(
                    onMailboxSelected = { mailboxId, mailboxName ->
                        backStack.add(Inbox(mailboxId, mailboxName))
                    },
                    onSignedOut = { backStack.add(Onboarding) },
                )
            }

            entry<Inbox> { key ->
                InboxScreen(
                    mailboxId = key.mailboxId,
                    mailboxName = key.mailboxName,
                )
            }
        },
    )
}
