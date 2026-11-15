package com.sonicstarsolutions.agentic.inbox.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.sonicstarsolutions.agentic.inbox.theme.AirmailDimens
import com.sonicstarsolutions.agentic.inbox.theme.airmail

private const val SLIDE_MILLIS = 500

/**
 * The app's progress line: a 6dp strip of navy and red diagonal stripes, drawn on a canvas. At
 * rest it is static. While [active] it slides one stripe period every 500ms, linear. With system
 * animations off it holds still and dims instead.
 *
 * Place it directly under a top app bar (inbox, composer) or across the top of onboarding — never
 * more than one on a screen. See "AirmailBand" in docs/DESIGN_SYSTEM.md.
 */
@Composable
fun AirmailBand(active: Boolean, modifier: Modifier = Modifier) {
    val navy = MaterialTheme.airmail.airmailNavy
    val red = MaterialTheme.airmail.airmailRed
    val paper = MaterialTheme.colorScheme.surface
    val reduceMotion = rememberReduceMotion()
    val phase = bandPhase(slide = active && !reduceMotion)
    val stripe = remember { Path() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(AirmailDimens.bandHeight)
            .alpha(if (active && reduceMotion) DIMMED_ALPHA else 1f)
            .then(
                if (active) {
                    Modifier.semantics {
                        contentDescription = "Loading"
                        progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                    }
                } else {
                    Modifier.clearAndSetSemantics { }
                },
            ),
    ) {
        drawRect(paper)
        val height = size.height
        val period = AirmailDimens.bandPeriod.toPx()
        val navyWidth = AirmailDimens.bandNavyStripe.toPx()
        val redWidth = AirmailDimens.bandRedStripe.toPx()
        val navyToRed = navyWidth + AirmailDimens.bandGap.toPx()

        // Stripes rise to the right (-45°): a parallelogram whose top edge is shifted by the
        // band's height. Start two periods back so the slide never uncovers the left edge.
        fun drawStripe(left: Float, width: Float, color: androidx.compose.ui.graphics.Color) {
            stripe.rewind()
            stripe.moveTo(left, height)
            stripe.lineTo(left + width, height)
            stripe.lineTo(left + width + height, 0f)
            stripe.lineTo(left + height, 0f)
            stripe.close()
            drawPath(stripe, color)
        }

        clipRect {
            var left = -2 * period + phase.value * period
            while (left < size.width) {
                drawStripe(left, navyWidth, navy)
                drawStripe(left + navyToRed, redWidth, red)
                left += period
            }
        }
    }
}

/** 0..1 across one stripe period; held at 0 whenever the band should not slide. */
@Composable
private fun bandPhase(slide: Boolean): State<Float> =
    if (slide) {
        rememberInfiniteTransition(label = "airmailBand").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(SLIDE_MILLIS, easing = LinearEasing), RepeatMode.Restart),
            label = "airmailBandPhase",
        )
    } else {
        remember { mutableStateOf(0f) }
    }

private const val DIMMED_ALPHA = 0.5f
