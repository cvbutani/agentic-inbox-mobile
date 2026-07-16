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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonicstarsolutions.agentic.inbox.domain.model.EmailSummary
import com.sonicstarsolutions.agentic.inbox.util.HtmlTextExtractor
import kotlin.math.absoluteValue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
    val surfaceColor = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        isUnread -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = surfaceColor,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header row: sender avatar, sender, time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar, or a checkbox in place of it while selecting
                if (selectionMode) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.size(40.dp),
                    )
                } else {
                    SenderAvatar(
                        sender = email.sender,
                        modifier = Modifier.size(40.dp),
                    )
                }

                // Sender + subject area
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = parseDisplayName(email.sender),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatRelativeTime(email.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Text(
                        text = email.subject,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (email.snippet?.contains("attachment") == true || email.snippet?.contains("📎") == true) {
                            Icon(
                                imageVector = Icons.Default.Attachment,
                                contentDescription = "Has attachment",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Star toggle — hidden while selecting so it doesn't compete with the checkbox tap target
                if (!selectionMode) {
                    IconButton(onClick = onToggleStarred, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (email.starred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (email.starred) "Unstar" else "Star",
                            tint = if (email.starred) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Snippet row — the server doesn't guarantee this is pre-stripped of HTML, and this
            // Text() can't render markup, so always reduce it to plain text first.
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
    }
}

@Composable
private fun SenderAvatar(
    sender: String,
    modifier: Modifier = Modifier,
) {
    val initials = parseDisplayName(sender)
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .take(2)

    val colors = listOf(
        0xFFEF5350.toInt(), 0xFFEC407A.toInt(), 0xFFAB47BC.toInt(),
        0xFF7E57C2.toInt(), 0xFF5C6BC0.toInt(), 0xFF42A5F5.toInt(),
        0xFF29B6F6.toInt(), 0xFF26C6DA.toInt(), 0xFF26A69A.toInt(),
        0xFF66BB6A.toInt(), 0xFF9CCC65.toInt(), 0xFFD4E157.toInt(),
    )
    val colorIndex = sender.hashCode().absoluteValue % colors.size
    val backgroundColor = Color(colors[colorIndex])

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
    }
}

private fun parseDisplayName(sender: String): String {
    // Parse "Name <email@domain.com>" format
    val angleBracketIndex = sender.indexOf('<')
    if (angleBracketIndex > 0) {
        return sender.substring(0, angleBracketIndex).trim()
    }
    return sender
}

private val MONTH_ABBREVIATIONS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private fun formatRelativeTime(dateString: String): String {
    return try {
        val date = Instant.parse(dateString)
        val diff = (Clock.System.now() - date).inWholeMilliseconds
        when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000}m"
            diff < 86_400_000 -> "${diff / 3_600_000}h"
            diff < 604_800_000 -> "${diff / 86_400_000}d"
            else -> {
                val local = date.toLocalDateTime(TimeZone.currentSystemDefault())
                "${MONTH_ABBREVIATIONS[local.month.ordinal]} ${local.day}"
            }
        }
    } catch (e: Exception) {
        dateString.take(10)
    }
}
