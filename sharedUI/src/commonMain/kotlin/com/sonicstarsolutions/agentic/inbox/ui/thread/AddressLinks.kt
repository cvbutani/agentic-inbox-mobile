package com.sonicstarsolutions.agentic.inbox.ui.thread

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonicstarsolutions.agentic.inbox.util.EmailAddressUtils

/** What picking a person's name does: the menu offers these two. */
internal class AddressActions(
    val onCopy: (address: String) -> Unit,
    /** Search the mailbox for mail sent by this address. */
    val onSearchFrom: (address: String) -> Unit,
    /** Search the mailbox for mail mentioning this address anywhere (To, Cc, Reply-To). */
    val onSearchFor: (address: String) -> Unit,
)

/** One header line — "to", "cc" or "reply-to" — with each person on it tappable. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddressLine(
    label: String,
    people: List<EmailAddressUtils.Person>,
    ownEmail: String?,
    /** True for the From line: its menu searches by sender rather than for the address anywhere. */
    isSender: Boolean,
    actions: AddressActions,
    modifier: Modifier = Modifier,
) {
    if (people.isEmpty()) return
    Row(modifier = modifier, verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.minimumInteractiveComponentSize().padding(0.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.Center) {
            people.forEachIndexed { index, person ->
                AddressLink(
                    person = person,
                    ownEmail = ownEmail,
                    isSender = isSender,
                    actions = actions,
                    trailingComma = index < people.lastIndex,
                )
            }
        }
    }
}

/** A person's name as a tap target. Tapping opens a small menu: copy the address, or search for it. */
@Composable
internal fun AddressLink(
    person: EmailAddressUtils.Person,
    ownEmail: String?,
    isSender: Boolean,
    actions: AddressActions,
    modifier: Modifier = Modifier,
    trailingComma: Boolean = false,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodySmall,
) {
    var open by remember { mutableStateOf(false) }
    val own = ownEmail != null && person.address.equals(ownEmail, ignoreCase = true)
    val shown = if (own && person.name == null) "You" else person.label
    Box(modifier = modifier) {
        Text(
            text = if (trailingComma) "$shown," else shown,
            style = style,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clickable(role = Role.Button, onClickLabel = "Address options for ${person.address}") { open = true }
                .minimumInteractiveComponentSize(),
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            // The address itself heads the menu: names can be misleading, the address is what is copied.
            Text(
                text = person.address,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            DropdownMenuItem(
                text = { Text("Copy address") },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                onClick = { open = false; actions.onCopy(person.address) },
            )
            DropdownMenuItem(
                text = { Text(if (isSender) "Search mail from $shown" else "Search for $shown") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                onClick = {
                    open = false
                    if (isSender) actions.onSearchFrom(person.address) else actions.onSearchFor(person.address)
                },
            )
        }
    }
}
