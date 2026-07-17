package com.sonicstarsolutions.agentic.inbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.absoluteValue

/**
 * A colored circle bearing up to two initials, hashed from [name] so the same name always lands
 * on the same color across the app — used for sender avatars in the inbox and mailbox avatars in
 * the mailbox picker, so both read as the same design language rather than two hand-rolled ones.
 */
@Composable
fun InitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
) {
    val initials = name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .take(2)

    val (backgroundColor, contentColor) = AVATAR_COLORS[name.hashCode().absoluteValue % AVATAR_COLORS.size]

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initials, color = contentColor, style = textStyle)
    }
}

/** Tonal container/on-container pairs (light ~tone-85 fill, dark ~tone-25 text of the same hue),
 * fixed rather than theme-derived so a sender keeps one identity color in light and dark mode.
 * Every pair clears WCAG AA (≥ 4.5:1) — the old flat palette put white text on lime. */
private val AVATAR_COLORS = listOf(
    Color(0xFFFFDAD6) to Color(0xFF8C1D18), // red
    Color(0xFFFFD8E4) to Color(0xFF7D2949), // pink
    Color(0xFFEADDFF) to Color(0xFF4F378B), // purple
    Color(0xFFE8DEF8) to Color(0xFF4A4458), // lavender
    Color(0xFFDEE0FF) to Color(0xFF3F4796), // indigo
    Color(0xFFD3E4FF) to Color(0xFF004A77), // blue
    Color(0xFFC2E8FF) to Color(0xFF004D65), // cyan
    Color(0xFFCCF7EF) to Color(0xFF00504A), // teal
    Color(0xFFC4EED0) to Color(0xFF0F5223), // green
    Color(0xFFDDEDC8) to Color(0xFF33531B), // olive
    Color(0xFFFFDF9E) to Color(0xFF5C4200), // amber
    Color(0xFFFFDBC8) to Color(0xFF723B16), // sienna
)
