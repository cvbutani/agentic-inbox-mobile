package com.sonicstarsolutions.agentic.inbox.ui.components

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonicstarsolutions.agentic.inbox.domain.model.Label

/** A label's name on its tonal colour. Long names are cut so a row of chips stays one line. */
@Composable
fun LabelChip(label: Label, modifier: Modifier = Modifier) {
    val (container, content) = labelColorPair(label.color)
    Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = container, contentColor = content) {
        Text(
            text = label.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp).padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
