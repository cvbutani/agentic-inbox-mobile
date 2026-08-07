package com.sonicstarsolutions.agentic.inbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sonicstarsolutions.agentic.inbox.theme.ListRowDimens

/** Placeholder for one email row while a list loads: same 72dp grid and row height rhythm as
 * [EmailListItem], drawn as mute shapes. Used by the inbox and search first-page loads. */
@Composable
fun SkeletonEmailRow(modifier: Modifier = Modifier) {
    val shapeColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = ListRowDimens.horizontalPadding,
                vertical = ListRowDimens.verticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(ListRowDimens.contentGap),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(ListRowDimens.avatarSize)
                .clip(CircleShape)
                .background(shapeColor),
        )
        Column(verticalArrangement = Arrangement.spacedBy(ListRowDimens.trailingGap)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shapeColor),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shapeColor),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shapeColor),
            )
        }
    }
}
