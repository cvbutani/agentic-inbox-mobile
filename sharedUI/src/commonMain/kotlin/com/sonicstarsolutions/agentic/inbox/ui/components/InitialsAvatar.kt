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

    val backgroundColor = AVATAR_COLORS[name.hashCode().absoluteValue % AVATAR_COLORS.size]

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initials, color = Color.White, style = textStyle)
    }
}

private val AVATAR_COLORS = listOf(
    Color(0xFFEF5350), Color(0xFFEC407A), Color(0xFFAB47BC),
    Color(0xFF7E57C2), Color(0xFF5C6BC0), Color(0xFF42A5F5),
    Color(0xFF29B6F6), Color(0xFF26C6DA), Color(0xFF26A69A),
    Color(0xFF66BB6A), Color(0xFF9CCC65), Color(0xFFD4E157),
)
