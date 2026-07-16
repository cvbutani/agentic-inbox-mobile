package com.sonicstarsolutions.agentic.inbox.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.ui.components.EmailListItem
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    mailboxId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onEmailSelected: (EmailSummary) -> Unit = {},
    viewModel: SearchViewModel = koinViewModel { parametersOf(mailboxId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var showFilters by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeError()
    }

    if (showFilters) {
        SearchFilterSheet(
            state = state,
            viewModel = viewModel,
            onDismiss = { showFilters = false },
            onApply = {
                showFilters = false
                viewModel.search()
            },
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = state.queryText,
                        onValueChange = viewModel::onQueryChanged,
                        placeholder = { Text("Search mail") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Search,
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                viewModel.search()
                                keyboardController?.hide()
                            },
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filters")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
        ) {
            when {
                state.loading && state.results.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                !state.hasSearched -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp),
                        )
                        Text(
                            text = "Search your mail",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                state.results.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No results found",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    items(state.results, key = { it.id }) { email ->
                        EmailListItem(
                            email = email,
                            onClick = { onEmailSelected(email) },
                            onLongClick = {},
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }

                    item {
                        if (state.loading && state.results.size < state.totalCount) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        } else if (state.results.size >= state.totalCount && state.totalCount > 0) {
                            Text(
                                text = "End of results",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchFilterSheet(
    state: SearchUiState,
    viewModel: SearchViewModel,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Filters", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = state.from,
                onValueChange = viewModel::onFromChanged,
                label = { Text("From") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.to,
                onValueChange = viewModel::onToChanged,
                label = { Text("To") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.subject,
                onValueChange = viewModel::onSubjectChanged,
                label = { Text("Subject") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.dateStart,
                onValueChange = viewModel::onDateStartChanged,
                label = { Text("From date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.dateEnd,
                onValueChange = viewModel::onDateEndChanged,
                label = { Text("To date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            TriStateFilterRow(label = "Read", value = state.isRead, onChange = viewModel::onReadFilterChanged)
            TriStateFilterRow(label = "Starred", value = state.isStarred, onChange = viewModel::onStarredFilterChanged)
            TriStateFilterRow(label = "Has attachment", value = state.hasAttachment, onChange = viewModel::onAttachmentFilterChanged)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = { viewModel.clearFilters() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Clear filters") }

                Button(
                    onClick = onApply,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Apply") }
            }
        }
    }
}

@Composable
private fun TriStateFilterRow(
    label: String,
    value: Boolean?,
    onChange: (Boolean?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = value == null,
                onClick = { onChange(null) },
                label = { Text("Any") },
            )
            FilterChip(
                selected = value == true,
                onClick = { onChange(true) },
                label = { Text("Yes") },
            )
            FilterChip(
                selected = value == false,
                onClick = { onChange(false) },
                label = { Text("No") },
            )
        }
    }
}
