package com.sonicstarsolutions.agentic.inbox.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.sonicstarsolutions.agentic.inbox.theme.AirmailDimens
import com.sonicstarsolutions.agentic.inbox.theme.ListRowDimens
import com.sonicstarsolutions.agentic.inbox.util.EmailTimeFormatter
import kotlinx.datetime.LocalDate

/** "JUL": the three-letter uppercase month a postmark carries. */
internal fun postmarkMonth(date: LocalDate): String =
    EmailTimeFormatter.MONTH_ABBREVIATIONS[date.month.ordinal].uppercase()

/**
 * A 40dp ring holding a day of the month over a three-letter month, in navy. It marks a date that
 * leads a group or an event, and fills the list's 40dp leading slot so the text beside it starts
 * on the 72dp edge. It is a stamp, not text: its glyphs stay at a fixed size so the ring never
 * has to outgrow 40dp, while the day name beside it scales with the system font.
 *
 * Pass `Modifier.clearAndSetSemantics { }` when the date is already spoken by adjacent text.
 */
@Composable
fun Postmark(date: LocalDate, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1f)) {
        Column(
            modifier = modifier
                .size(ListRowDimens.avatarSize)
                .border(AirmailDimens.postmarkStroke, ink, CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = date.day.toString(),
                color = ink,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                text = postmarkMonth(date),
                color = ink,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.04.em,
                ),
            )
        }
    }
}
