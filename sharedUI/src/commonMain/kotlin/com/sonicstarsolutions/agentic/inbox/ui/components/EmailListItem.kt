package com.sonicstarsolutions.agentic.inbox.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.util.EmailAddressUtils
import com.sonicstarsolutions.agentic.inbox.util.EmailTimeFormatter
import com.sonicstarsolutions.agentic.inbox.util.HtmlTextExtractor

/**
 * One email row on the shared 72dp list grid: a 40dp avatar in a 16dp gutter, then every line of
 * text — sender, subject, snippet — sharing a single left edge that matches the list divider
 * inset. Unread is signalled by weight and time color alone; a background tint stays reserved
 * for [selected] so the two states never blur together.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmailListItem(
    email: EmailSummary,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleStarred: () -> Unit = {},
    selectionMode: Boolean = false,
    selected: Boolean = false,
) {
    // A thread's latest message can be read while earlier messages in it aren't — thread_unread_count
    // catches that; a plain !email.read alone would show the row as read even with unread messages
    // still in the conversation.
    val isUnread = email.isUnread()
    val contentWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Avatar, or a checkbox in place of it while selecting
            if (selectionMode) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.size(40.dp),
                )
            } else {
                InitialsAvatar(
                    name = EmailAddressUtils.displayName(email.sender, ownEmail = null),
                    modifier = Modifier.size(40.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = EmailAddressUtils.displayName(email.sender, ownEmail = null),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = contentWeight),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = EmailTimeFormatter.format(email.date),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = contentWeight),
                        color = if (isUnread) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = email.subject,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = contentWeight),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        // The server doesn't guarantee the snippet is pre-stripped of HTML, and
                        // this Text() can't render markup, so always reduce it to plain text first.
                        email.snippet?.let { snippet ->
                            Text(
                                text = HtmlTextExtractor.toPlainText(snippet),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Star toggle — hidden while selecting so it doesn't compete with the checkbox
                    // tap target. No size override: IconButton's default 48dp minimum touch target
                    // is the accessibility floor.
                    if (!selectionMode) {
                        IconButton(onClick = onToggleStarred) {
                            Icon(
                                imageVector = if (email.starred) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (email.starred) "Unstar" else "Star",
                                tint = if (email.starred) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
