package com.sonicstarsolutions.agentic.inbox.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicstarsolutions.agentic.inbox.ui.components.SectionTitle
import org.koin.compose.viewmodel.koinViewModel

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle("Connect your Worker")
            Text(
                "Enter the Worker URL and your Cloudflare Access service-token credentials. " +
                    "They are stored locally on this device.",
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedTextField(
                value = state.baseUrl,
                onValueChange = viewModel::onBaseUrlChanged,
                label = { Text("Worker URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                isError = state.errorMessage != null && state.baseUrl.isBlank(),
            )

            OutlinedTextField(
                value = state.clientId,
                onValueChange = viewModel::onClientIdChanged,
                label = { Text("CF-Access-Client-Id") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = state.errorMessage != null && state.clientId.isBlank(),
            )

            OutlinedTextField(
                value = state.clientSecret,
                onValueChange = viewModel::onClientSecretChanged,
                label = { Text("CF-Access-Client-Secret") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = state.errorMessage != null && state.clientSecret.isBlank(),
            )

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = viewModel::validateAndSave,
                enabled = !state.validating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.validating) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                } else {
                    Text("Connect")
                }
            }
        }
    }
}
