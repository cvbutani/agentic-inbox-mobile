package com.sonicstarsolutions.agentic.inbox.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.theme.ListRowDimens
import com.sonicstarsolutions.agentic.inbox.theme.airmail
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
    /** The time label. Rows under a day header pass [EmailTimeFormatter.formatClock] (the header
     * carries the day); rows with no header, like search results, keep the default. */
    timeText: String = EmailTimeFormatter.format(email.date),
) {
    // A thread's latest message can be read while earlier messages in it aren't — thread_unread_count
    // catches that; a plain !email.read alone would show the row as read even with unread messages
    // still in the conversation.
    val isUnread = email.isUnread()
    val contentWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal
    // A container color always pairs with its own "on" color: a selected row sets all of its
    // secondary text and the star to onPrimaryContainer, never onSurfaceVariant.
    val secondaryColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics {
                // The row is the control, so it must say what the check circle shows.
                if (selectionMode) stateDescription = if (selected) "Selected" else "Not selected"
            },
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = ListRowDimens.horizontalPadding,
                vertical = ListRowDimens.verticalPadding,
            ),
            horizontalArrangement = Arrangement.spacedBy(ListRowDimens.contentGap),
            verticalAlignment = Alignment.Top,
        ) {
            // A selected row swaps its avatar for a navy check; unselected rows keep theirs, so
            // while selecting you still see who each message is from.
            if (selected && selectionMode) {
                Box(
                    modifier = Modifier
                        .size(ListRowDimens.avatarSize)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            } else {
                InitialsAvatar(
                    name = EmailAddressUtils.displayName(email.sender, ownEmail = null),
                    modifier = Modifier.size(ListRowDimens.avatarSize),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(ListRowDimens.lineGap),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ListRowDimens.trailingGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = EmailAddressUtils.displayName(email.sender, ownEmail = null),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = contentWeight),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = email.threadCount <= 1),
                    )
                    // A conversation row stands for several messages: say how many, as plain muted
                    // text like the drawer's unread counts.
                    if (email.threadCount > 1) {
                        Text(
                            text = email.threadCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).semantics { contentDescription = "${email.threadCount} messages" },
                        )
                    }
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = contentWeight),
                        color = if (isUnread && !selected) MaterialTheme.colorScheme.primary else secondaryColor,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ListRowDimens.trailingGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(ListRowDimens.lineGap),
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
                            val preview = remember(snippet) { HtmlTextExtractor.toPlainText(snippet) }
                            Text(
                                text = preview,
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (email.labels.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                email.labels.take(MAX_ROW_LABELS).forEach { label ->
                                    LabelChip(label, modifier = Modifier.weight(1f, fill = false))
                                }
                                val hidden = email.labels.size - MAX_ROW_LABELS
                                if (hidden > 0) {
                                    Text(
                                        text = "+$hidden",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    // Star toggle — hidden while selecting so it doesn't compete with the row's own
                    // tap target. No size override: IconButton's default 48dp minimum touch target
                    // is the accessibility floor.
                    if (!selectionMode) {
                        IconButton(onClick = onToggleStarred) {
                            Icon(
                                imageVector = if (email.starred) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (email.starred) "Unstar" else "Star",
                                tint = if (email.starred && !selected) {
                                    MaterialTheme.airmail.star
                                } else {
                                    secondaryColor
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

/** Chips shown on a row before the rest collapse into "+N" — enough to scan, few enough for one line. */
private const val MAX_ROW_LABELS = 3
