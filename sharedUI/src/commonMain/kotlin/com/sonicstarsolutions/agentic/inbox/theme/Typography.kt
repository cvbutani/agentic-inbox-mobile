package com.sonicstarsolutions.agentic.inbox.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font as fontResource
import agentic_inbox.sharedui.generated.resources.Res
import agentic_inbox.sharedui.generated.resources.open_sans
import agentic_inbox.sharedui.generated.resources.open_sans_italic

/**
 * Open Sans ships from Google Fonts as a single variable file per style (weight axis 300–800)
 * rather than one file per weight, so each entry below points at the same [Res.font.open_sans] /
 * [Res.font.open_sans_italic] resource and asks for a specific instance via
 * [FontVariation.weight] — that's what actually selects the weight, not the `weight =` argument
 * alone (which only affects style-matching, e.g. what `FontWeight.Bold` in a `TextStyle` resolves
 * to).
 *
 * On Android 7.0/7.1 (API 24–25) the OS can't interpolate a variable font's axes, so every weight
 * below renders at the file's default instance instead — text stays legible, just without the
 * bold/regular distinction. Every other target (API 26+, iOS, desktop) renders the requested
 * weight correctly.
 */
@Composable
private fun openSansFontFamily(): FontFamily = FontFamily(
    fontResource(Res.font.open_sans, weight = FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    fontResource(Res.font.open_sans, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    fontResource(Res.font.open_sans, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    fontResource(Res.font.open_sans, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    fontResource(Res.font.open_sans, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    fontResource(Res.font.open_sans, weight = FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
    fontResource(
        Res.font.open_sans_italic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    fontResource(
        Res.font.open_sans_italic,
        weight = FontWeight.SemiBold,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    fontResource(
        Res.font.open_sans_italic,
        weight = FontWeight.Bold,
        style = FontStyle.Italic,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

/**
 * Material 3's default type scale (sizes, line heights, letter spacing) with every slot's
 * typeface swapped to Open Sans — built from [baseline] rather than hand-specifying all 15 slots,
 * so the scale itself stays exactly what M3 recommends.
 */
@Composable
internal fun appTypography(baseline: Typography = Typography()): Typography {
    val fontFamily = openSansFontFamily()
    return Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = fontFamily),
        displayMedium = baseline.displayMedium.copy(fontFamily = fontFamily),
        displaySmall = baseline.displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = baseline.titleLarge.copy(fontFamily = fontFamily),
        titleMedium = baseline.titleMedium.copy(fontFamily = fontFamily),
        titleSmall = baseline.titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = baseline.bodySmall.copy(fontFamily = fontFamily),
        labelLarge = baseline.labelLarge.copy(fontFamily = fontFamily),
        labelMedium = baseline.labelMedium.copy(fontFamily = fontFamily),
        labelSmall = baseline.labelSmall.copy(fontFamily = fontFamily),
    )
}
