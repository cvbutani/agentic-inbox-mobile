package com.sonicstarsolutions.agentic.inbox.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import kotlin.math.roundToInt

/**
 * A top app bar with the [AirmailBand] directly beneath it, as one unit for a Scaffold's `topBar`.
 *
 * When the bar hides on scroll ([scrollBehavior]), the band is reclaimed with it: a band left
 * behind would sit alone at the top of the screen. The band's visible height follows the bar's
 * collapsed fraction, so it slides away and returns together with the bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithBand(
    bandActive: Boolean,
    modifier: Modifier = Modifier,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    bar: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            bar()
            AirmailBand(active = bandActive)
        },
    ) { measurables, constraints ->
        val barPlaceable = measurables[0].measure(constraints)
        val bandPlaceable = measurables[1].measure(constraints)
        val collapsed = scrollBehavior?.state?.collapsedFraction ?: 0f
        val bandHeight = (bandPlaceable.height * (1f - collapsed)).roundToInt()
        layout(barPlaceable.width, barPlaceable.height + bandHeight) {
            barPlaceable.place(0, 0)
            bandPlaceable.place(0, barPlaceable.height)
        }
    }
}
