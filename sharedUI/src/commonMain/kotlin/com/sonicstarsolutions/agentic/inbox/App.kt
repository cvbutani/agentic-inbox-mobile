package com.sonicstarsolutions.agentic.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicstarsolutions.agentic.inbox.data.settings.CredentialsRepository
import com.sonicstarsolutions.agentic.inbox.di.allModules
import com.sonicstarsolutions.agentic.inbox.theme.AppTheme
import com.sonicstarsolutions.agentic.inbox.ui.mailbox.picker.MailboxPickerScreen
import com.sonicstarsolutions.agentic.inbox.ui.onboarding.OnboardingScreen
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.module.Module

@Composable
fun App(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
    appModule: List<Module> = emptyList()
) {
    KoinApplication(
        application = {
            modules(allModules + appModule)
        }
    ) {
        AppTheme(onThemeChanged = onThemeChanged) {
            AppRoot()
        }
    }
}

@Composable
private fun AppRoot() {
    val credentials: CredentialsRepository = koinInject()
    val credentialsState by credentials.state.collectAsStateWithLifecycle()
    var selectedMailboxId by remember { mutableStateOf<String?>(null) }

    when {
        selectedMailboxId != null -> MailboxPlaceholder(selectedMailboxId!!) {
            selectedMailboxId = null
        }

        !credentialsState.isComplete() -> OnboardingScreen(onSaved = { /* credentials flip the state */ })

        else -> MailboxPickerScreen(
            onMailboxSelected = { selectedMailboxId = it },
            onSignOut = { credentials.clear() },
        )
    }
}

@Composable
private fun MailboxPlaceholder(mailboxId: String, onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Mailbox $mailboxId selected.\nM1 lands the inbox list here.",
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(onClick = onBack) { Text("Back to picker") }
        }
    }
}
