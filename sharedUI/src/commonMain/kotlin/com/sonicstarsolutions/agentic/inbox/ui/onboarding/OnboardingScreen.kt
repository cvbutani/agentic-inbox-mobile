package com.sonicstarsolutions.agentic.inbox.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicstarsolutions.agentic.inbox.ui.components.ErrorBanner
import org.koin.compose.viewmodel.koinViewModel

/** Long-form text stays readable up to roughly this width; past it, fields just look stretched. */
private val FORM_MAX_WIDTH = 460.dp

@Composable
fun OnboardingScreen(
    onSaved: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) {
            viewModel.consumeSaved()
            onSaved()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        // Insets are applied per layout, on content rather than here, so the expanded layout's
        // hero pane can run its background edge to edge behind the system bars. Measuring the
        // full window (not the inset area) is also what the window size classes are defined on.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            when (onboardingLayoutFor(maxWidth, maxHeight)) {
                OnboardingLayout.COMPACT -> CompactOnboarding(state, viewModel)
                OnboardingLayout.MEDIUM -> MediumOnboarding(state, viewModel)
                OnboardingLayout.EXPANDED -> ExpandedOnboarding(state, viewModel)
            }
        }
    }
}

/** Phone: one column, edge to edge. No card — at this width its inset would only cost usable space. */
@Composable
private fun CompactOnboarding(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        BrandMark(size = 56.dp)
        Headline(centred = false)
        ConnectionForm(state = state, viewModel = viewModel)
        PrivacyNote()
    }
}

/** Tablet portrait: the same form, but capped and centred inside a card so it reads as a
 * deliberate panel rather than a phone layout stretched to fit. */
@Composable
private fun MediumOnboarding(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        FormCard(modifier = Modifier.widthIn(max = FORM_MAX_WIDTH + 48.dp)) {
            BrandMark(size = 56.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Headline(centred = true)
            ConnectionForm(state = state, viewModel = viewModel)
            PrivacyNote(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

/** Tablet landscape: the explanation moves into its own pane so the form isn't a lone column
 * marooned in the middle of a very wide window. */
@Composable
private fun ExpandedOnboarding(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Deliberately not inset: its background runs to the window edges, and the pane insets
        // its own content instead.
        HeroPane(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 40.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = FORM_MAX_WIDTH),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Text(
                    text = "Connect your Worker",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                ConnectionForm(state = state, viewModel = viewModel)
                PrivacyNote()
            }
        }
    }
}

@Composable
private fun HeroPane(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                BrandMark(size = 72.dp)
                Text(
                    text = "Agentic Inbox",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Your own mail, on your own Worker. Point the app at your deployment " +
                        "and everything stays between this device and your Cloudflare account.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BrandMark(size: Dp, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size / 2),
            )
        }
    }
}

@Composable
private fun Headline(centred: Boolean) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = if (centred) Alignment.CenterHorizontally else Alignment.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // SemiBold, and the same style the expanded layout uses — the same words shouldn't
        // change voice with window size, and Open Sans Bold is heavy at display sizes.
        Text(
            text = "Connect your Worker",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Point Agentic Inbox at your deployment to get started.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FormCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            content = content,
        )
    }
}

/**
 * The three fields, the error, and the submit button.
 *
 * The button stays enabled even when fields are empty: the ViewModel already reports exactly
 * what's missing, and a dead button explains nothing to someone who can't see which field they
 * skipped.
 */
@Composable
private fun ConnectionForm(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var secretVisible by rememberSaveable { mutableStateOf(false) }

    val submit = {
        keyboard?.hide()
        focusManager.clearFocus()
        viewModel.validateAndSave()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = state.baseUrl,
            onValueChange = viewModel::onBaseUrlChanged,
            label = { Text("Worker URL") },
            supportingText = { Text("https://your-worker.workers.dev") },
            leadingIcon = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            isError = state.errorMessage != null && state.baseUrl.isBlank(),
        )

        OutlinedTextField(
            value = state.clientId,
            onValueChange = viewModel::onClientIdChanged,
            label = { Text("Access Client ID") },
            supportingText = { Text("From your Cloudflare Access service token") },
            leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            isError = state.errorMessage != null && state.clientId.isBlank(),
        )

        OutlinedTextField(
            value = state.clientSecret,
            onValueChange = viewModel::onClientSecretChanged,
            label = { Text("Access Client Secret") },
            supportingText = { Text("Shown once when the token was created") },
            leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { secretVisible = !secretVisible }) {
                    Icon(
                        imageVector = if (secretVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (secretVisible) "Hide secret" else "Show secret",
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (secretVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            isError = state.errorMessage != null && state.clientSecret.isBlank(),
        )

        state.errorMessage?.let { message -> ErrorBanner(message) }

        Button(
            onClick = submit,
            enabled = !state.validating,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            if (state.validating) {
                // Fixed size, not fillMaxWidth: a spinner told to fill the button stretches into
                // an oval that reads as a rendering fault.
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text("Connect", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun PrivacyNote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = "Stored only on this device",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
