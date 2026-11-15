package com.sonicstarsolutions.agentic.inbox.ui.components

import androidx.compose.ui.graphics.Color
import com.sonicstarsolutions.agentic.inbox.domain.model.LabelColors

/**
 * Container/on-container pair for a label colour name. Fixed tonal pairs rather than theme
 * tokens — like the avatar palette — so a label keeps its colour in light and dark mode; every
 * pair clears 4.5:1 (see LabelColorsTest). A name this app doesn't know (a newer server may add
 * colours) renders as slate.
 */
internal fun labelColorPair(name: String): Pair<Color, Color> = LABEL_PAIRS[name] ?: LABEL_PAIRS.getValue(LabelColors.DEFAULT)

private val LABEL_PAIRS: Map<String, Pair<Color, Color>> = mapOf(
    "slate" to (Color(0xFFDDE1E6) to Color(0xFF2F3640)),
    "red" to (Color(0xFFFFDAD6) to Color(0xFF8C1D18)),
    "orange" to (Color(0xFFFFDBC8) to Color(0xFF723B16)),
    "amber" to (Color(0xFFFFDF9E) to Color(0xFF5C4200)),
    "green" to (Color(0xFFC4EED0) to Color(0xFF0F5223)),
    "teal" to (Color(0xFFCCF7EF) to Color(0xFF00504A)),
    "blue" to (Color(0xFFD3E4FF) to Color(0xFF004A77)),
    "purple" to (Color(0xFFEADDFF) to Color(0xFF4F378B)),
)
