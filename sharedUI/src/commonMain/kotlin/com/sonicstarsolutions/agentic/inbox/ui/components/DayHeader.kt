package com.sonicstarsolutions.agentic.inbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonicstarsolutions.agentic.inbox.theme.ListRowDimens
import com.sonicstarsolutions.agentic.inbox.util.DayGroup

private const val MAX_UNREAD_SHOWN = 99

/** "3 unread", capped at "99+ unread"; null when there is nothing unread to say. */
internal fun unreadLabel(count: Int): String? = when {
    count <= 0 -> null
    count > MAX_UNREAD_SHOWN -> "$MAX_UNREAD_SHOWN+ unread"
    else -> "$count unread"
}

/**
 * Heads one day of a list: a [Postmark], the day's name, and how many of its messages are unread.
 * On the 72dp grid, so the name starts where every row's text does. Sticky in lists (the
 * `surface` background lets rows pass under it); static between messages in a thread.
 */
@Composable
fun DayHeader(
    group: DayGroup<*>,
    unreadCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            .semantics(mergeDescendants = true) { heading() },
        horizontalArrangement = Arrangement.spacedBy(ListRowDimens.contentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Postmark(group.date, modifier = Modifier.clearAndSetSemantics { })
        Text(
            text = group.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        unreadLabel(unreadCount)?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}
