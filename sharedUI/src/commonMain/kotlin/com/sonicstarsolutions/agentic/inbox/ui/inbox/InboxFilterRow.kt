package com.sonicstarsolutions.agentic.inbox.ui.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sonicstarsolutions.agentic.inbox.domain.model.Label

/**
 * The strip of local filters above the list: "Unread", and — in a single mailbox that has labels —
 * a label picker. Both only narrow the rows already loaded; neither asks the server anything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InboxFilterRow(
    unreadOnly: Boolean,
    onUnreadOnlyChange: (Boolean) -> Unit,
    /** Empty hides the label chip: the domain-wide list has no labels, and neither does a mailbox without any. */
    labels: List<Label>,
    labelFilter: Label?,
    onLabelFilterChange: (Label?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "unread") {
            FilterChip(
                selected = unreadOnly,
                onClick = { onUnreadOnlyChange(!unreadOnly) },
                label = { Text("Unread") },
                leadingIcon = if (unreadOnly) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                } else {
                    null
                },
            )
        }
        if (labels.isNotEmpty()) {
            item(key = "label") {
                var open by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = labelFilter != null,
                        onClick = { open = true },
                        label = { Text(labelFilter?.name ?: "Label") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        DropdownMenuItem(
                            text = { Text("All labels") },
                            onClick = { open = false; onLabelFilterChange(null) },
                        )
                        labels.forEach { label ->
                            DropdownMenuItem(
                                text = { Text(label.name) },
                                leadingIcon = { LabelDot(label.color) },
                                trailingIcon = if (label.id == labelFilter?.id) {
                                    { Icon(Icons.Default.Check, contentDescription = "Selected") }
                                } else {
                                    null
                                },
                                onClick = { open = false; onLabelFilterChange(label) },
                            )
                        }
                    }
                }
            }
        }
    }
}
