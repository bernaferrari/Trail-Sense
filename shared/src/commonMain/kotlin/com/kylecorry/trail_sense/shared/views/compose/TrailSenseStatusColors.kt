package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.material3.dynamicColorScheme

/**
 * QuietGuard's semantic colors. Material 3 has no success or caution role, so derive them from their own seeds with
 * the same tonal rules as the main scheme: readable in light and dark, and next to `error` for "bad".
 */
@Immutable
data class TrailSenseStatusColors(
    val good: Color,
    val goodContainer: Color,
    val onGoodContainer: Color,
    val caution: Color,
    val cautionContainer: Color,
    val onCautionContainer: Color
)

private val GoodSeed = Color(0xFF2E7D32)
private val CautionSeed = Color(0xFFF9A825)

/** The status colors for the current theme's lightness. */
@Composable
fun trailSenseStatusColors(): TrailSenseStatusColors {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return remember(dark) {
        fun scheme(seed: Color) = dynamicColorScheme(
            seedColor = seed,
            isDark = dark,
            style = PaletteStyle.TonalSpot,
            specVersion = ColorSpec.SpecVersion.SPEC_2025
        )
        val good = scheme(GoodSeed)
        val caution = scheme(CautionSeed)
        TrailSenseStatusColors(
            good = good.primary,
            goodContainer = good.primaryContainer,
            onGoodContainer = good.onPrimaryContainer,
            caution = caution.primary,
            cautionContainer = caution.primaryContainer,
            onCautionContainer = caution.onPrimaryContainer
        )
    }
}
