package com.sonicstarsolutions.agentic.inbox.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.ui.components.EmailListItem
import com.sonicstarsolutions.agentic.inbox.ui.components.SkeletonEmailRow
import com.sonicstarsolutions.agentic.inbox.ui.components.StatusPane
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    var showFilters by remember { mutableStateOf(false) }

    // The user came here to type — don't make them tap the field first.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // Errors while results are on screen stay unobtrusive (snackbar); with nothing on screen the
    // error state below takes over instead.
    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        if (state.results.isNotEmpty()) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeError()
        }
    }

    // Infinite scroll: fetch the next page when the last visible row closes in on the list end.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible != null && lastVisible >= listState.layoutInfo.totalItemsCount - 3) {
                    viewModel.loadMore()
                }
            }
    }

    // Typing and reading don't overlap — scrolling the results puts the keyboard away.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling -> if (scrolling) keyboardController?.hide() }
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
                    // Borderless on purpose: the app bar itself is the search box, not a box
                    // within a box.
                    TextField(
                        value = state.queryText,
                        onValueChange = viewModel::onQueryChanged,
                        placeholder = { Text("Search in mail") },
                        singleLine = true,
                        trailingIcon = {
                            if (state.queryText.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = true }) {
                        BadgedBox(
                            badge = { if (state.hasActiveFilters) Badge() },
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        },
    ) { paddingValues ->
        // Capped and centered on wide windows: a results row stretched to a tablet's full
        // width puts the sender and its time a metre apart.
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.TopCenter,
        ) {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = CONTENT_MAX_WIDTH)) {
            if (state.hasActiveFilters) {
                ActiveFilterChips(state = state, viewModel = viewModel)
            }

            when {
                state.loading && state.results.isEmpty() -> Column(modifier = Modifier.fillMaxSize()) {
                    repeat(7) {
                        SkeletonEmailRow()
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }

                state.errorMessage != null && state.results.isEmpty() && state.hasSearched ->
                    StatusPane(
                        icon = Icons.Default.ErrorOutline,
                        title = "Something went wrong",
                        detail = state.errorMessage,
                    ) {
                        Button(onClick = { viewModel.search() }) { Text("Retry") }
                    }

                !state.hasSearched -> StatusPane(
                    icon = Icons.Default.Search,
                    title = "Search your mail",
                    detail = "Find emails by sender, subject, or keywords",
                )

                state.results.isEmpty() -> StatusPane(
                    icon = Icons.Default.SearchOff,
                    title = if (state.queryText.isBlank()) "No results" else "No results for “${state.queryText.trim()}”",
                    detail = "Try different keywords or filters",
                )

                else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            text = if (state.totalCount == 1) "1 result" else "${state.totalCount} results",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }

                    items(state.results, key = { it.id }) { email ->
                        Column(modifier = Modifier.animateItem()) {
                            EmailListItem(
                                email = email,
                                onClick = { onEmailSelected(email) },
                                onLongClick = {},
                                onToggleStarred = { viewModel.toggleStarred(email) },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }

                    item {
                        if (state.loadingMore) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

/** Content stops growing past this width on tablets — the same cap the mailbox grid uses. */
private val CONTENT_MAX_WIDTH = 840.dp

/** Every active filter as a removable chip, so what narrowed the results is visible — and
 * undoable — without reopening the sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveFilterChips(
    state: SearchUiState,
    viewModel: SearchViewModel,
) {
    data class ChipSpec(val label: String, val onRemove: () -> Unit)

    val chips = buildList {
        if (state.from.isNotBlank()) add(ChipSpec("From: ${state.from.trim()}") { viewModel.onFromChanged("") })
        if (state.to.isNotBlank()) add(ChipSpec("To: ${state.to.trim()}") { viewModel.onToChanged("") })
        if (state.subject.isNotBlank()) add(ChipSpec("Subject: ${state.subject.trim()}") { viewModel.onSubjectChanged("") })
        if (state.dateStart.isNotBlank()) add(ChipSpec("After ${state.dateStart.trim()}") { viewModel.onDateStartChanged("") })
        if (state.dateEnd.isNotBlank()) add(ChipSpec("Before ${state.dateEnd.trim()}") { viewModel.onDateEndChanged("") })
        state.isRead?.let { add(ChipSpec(if (it) "Read" else "Unread") { viewModel.onReadFilterChanged(null) }) }
        state.isStarred?.let { add(ChipSpec(if (it) "Starred" else "Not starred") { viewModel.onStarredFilterChanged(null) }) }
        state.hasAttachment?.let { add(ChipSpec(if (it) "Has attachment" else "No attachment") { viewModel.onAttachmentFilterChanged(null) }) }
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(chips.size) { index ->
            val chip = chips[index]
            InputChip(
                selected = true,
                onClick = {
                    chip.onRemove()
                    viewModel.search()
                },
                label = { Text(chip.label) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove filter",
                        modifier = Modifier.size(InputChipDefaults.IconSize),
                    )
                },
            )
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
                .verticalScroll(rememberScrollState())
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

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.dateStart,
                    onValueChange = viewModel::onDateStartChanged,
                    label = { Text("From date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    isError = state.dateError != null,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.dateEnd,
                    onValueChange = viewModel::onDateEndChanged,
                    label = { Text("To date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    isError = state.dateError != null,
                    modifier = Modifier.weight(1f),
                )
            }
            if (state.dateError != null) {
                Text(
                    text = state.dateError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

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
                    enabled = state.dateError == null,
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
