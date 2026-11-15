package com.sonicstarsolutions.agentic.inbox.ui.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonicstarsolutions.agentic.inbox.domain.model.Label
import com.sonicstarsolutions.agentic.inbox.domain.model.LabelColors
import com.sonicstarsolutions.agentic.inbox.ui.components.LabelChip
import com.sonicstarsolutions.agentic.inbox.ui.components.labelColorPair

/** The drawer's "Labels" block: one row per label plus "New label". */
@Composable
internal fun LabelDrawerSection(
    labels: List<Label>,
    selectedLabelId: String?,
    onLabelSelected: (Label) -> Unit,
    onCreateLabel: () -> Unit,
    onEditLabel: (Label) -> Unit,
    onDeleteLabel: (Label) -> Unit,
) {
    if (labels.isNotEmpty()) {
        Text(
            text = "Labels",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 4.dp),
        )
        labels.forEach { label ->
            LabelDrawerItem(
                label = label,
                selected = label.id == selectedLabelId,
                onClick = { onLabelSelected(label) },
                onEdit = { onEditLabel(label) },
                onDelete = { onDeleteLabel(label) },
            )
        }
    }
    NavigationDrawerItem(
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        label = { Text("New label") },
        selected = false,
        onClick = onCreateLabel,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun LabelDrawerItem(
    label: Label,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    NavigationDrawerItem(
        icon = { LabelDot(label.color) },
        label = { Text(text = label.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        badge = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (label.count > 0) {
                    Text(
                        text = if (label.count > 99) "99+" else label.count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    // Default 48dp IconButton keeps the touch-target floor; only the glyph is small.
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Label options",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, onClick = { showMenu = false; onEdit() })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() })
                    }
                }
            }
        },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

/** The drawer-icon stand-in for a chip: a 14dp dot with the pair's pastel fill and a 2dp ring in
 * its deep color, so it reads on both the light and the dark drawer. */
@Composable
internal fun LabelDot(color: String, modifier: Modifier = Modifier) {
    val (fill, ring) = labelColorPair(color)
    Box(
        modifier = modifier
            .size(14.dp)
            .background(fill, CircleShape)
            .border(2.dp, ring, CircleShape),
    )
}

/** Create (when [initial] is null) or edit a label: a name and one of the server's colours. */
@Composable
internal fun LabelEditorDialog(
    initial: Label?,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (name: String, color: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var color by rememberSaveable { mutableStateOf(initial?.color ?: LabelColors.DEFAULT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New label" else "Edit label") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Label name") },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                ColorPicker(selected = color, onSelected = { color = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, color) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ColorPicker(selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Two rows of four keep every 48dp swatch inside a dialog's width.
        LabelColors.all.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { name ->
                    val (container, content) = labelColorPair(name)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { onSelected(name) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(CircleShape).background(container),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (name == selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "$name (selected)",
                                    tint = content,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DeleteLabelDialog(labelName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete label") },
        text = { Text("“$labelName” will be removed from every message. The messages themselves stay.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Picks labels for the selected emails. Each box shows whether none, some or all of the selection
 * carries that label; tapping flips it, and "Apply" sends only the labels that changed.
 */
@Composable
internal fun LabelPickerDialog(
    labels: List<Label>,
    initial: (labelId: String) -> LabelCheck,
    onDismiss: () -> Unit,
    onApply: (Map<String, Boolean>) -> Unit,
) {
    val start = remember(labels) { labels.associate { it.id to initial(it.id) } }
    var current by remember(labels) { mutableStateOf(start) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Label as") },
        text = {
            if (labels.isEmpty()) {
                Text("No labels yet. Create one from the menu first.")
            } else {
                LazyColumn {
                    items(labels, key = { it.id }) { label ->
                        val check = current.getValue(label.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { current = current + (label.id to nextCheck(check)) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TriStateCheckbox(
                                state = when (check) {
                                    LabelCheck.ALL -> ToggleableState.On
                                    LabelCheck.SOME -> ToggleableState.Indeterminate
                                    LabelCheck.NONE -> ToggleableState.Off
                                },
                                onClick = null,
                                modifier = Modifier.padding(12.dp),
                            )
                            LabelChip(label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(labelChanges(start, current)) }, enabled = labels.isNotEmpty()) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** A tap turns "none" and "some" on and "all" off — a mixed box resolves to the whole selection first. */
internal fun nextCheck(check: LabelCheck): LabelCheck = if (check == LabelCheck.ALL) LabelCheck.NONE else LabelCheck.ALL

/** The labels whose box the user changed, mapped to whether they are now on. A box still mixed
 * (never tapped, or tapped back) is not a change. */
internal fun labelChanges(start: Map<String, LabelCheck>, end: Map<String, LabelCheck>): Map<String, Boolean> =
    end.filter { (id, check) -> check != start[id] && check != LabelCheck.SOME }
        .mapValues { it.value == LabelCheck.ALL }
