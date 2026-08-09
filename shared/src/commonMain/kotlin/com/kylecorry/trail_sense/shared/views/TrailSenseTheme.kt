package com.kylecorry.trail_sense.shared.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

/**
 * Trail Sense Design System — Material 3 Expressive + Material Kolor
 *
 * Supports:
 * - User-selected accent colors (Orange, Blue, Green, Red, Purple, Teal)
 * - True Android Dynamic Color (wallpaper-based Material You) via [overrideColorScheme]
 * - Excellent light/dark + AMOLED harmony via Material Kolor
 *
 * This is the single source of truth for the app's look.
 */
sealed interface TrailSenseColorScheme {
    data object Orange : TrailSenseColorScheme
    data object Blue : TrailSenseColorScheme
    data object Green : TrailSenseColorScheme
    data object Red : TrailSenseColorScheme
    data object Purple : TrailSenseColorScheme
    data object Teal : TrailSenseColorScheme

    /**
     * Use the system's Dynamic Color (Android 12+ wallpaper-based Material You).
     * Falls back gracefully on older Android or non-Android platforms.
     */
    data object Dynamic : TrailSenseColorScheme

    companion object {
        val entries: List<TrailSenseColorScheme> = listOf(
            Orange, Blue, Green, Red, Purple, Teal, Dynamic
        )

        fun fromId(id: Int): TrailSenseColorScheme = when (id) {
            0 -> Orange
            1 -> Blue
            2 -> Green
            3 -> Red
            4 -> Purple
            5 -> Teal
            6 -> Dynamic
            else -> Orange
        }

        fun toId(scheme: TrailSenseColorScheme): Int = when (scheme) {
            Orange -> 0
            Blue -> 1
            Green -> 2
            Red -> 3
            Purple -> 4
            Teal -> 5
            Dynamic -> 6
        }
    }
}

private fun TrailSenseColorScheme.toSeedColor(): Color = when (this) {
    TrailSenseColorScheme.Orange -> Color(0xFFFF5A1F)
    TrailSenseColorScheme.Blue   -> Color(0xFF1565C0)
    TrailSenseColorScheme.Green  -> Color(0xFF2E7D32)
    TrailSenseColorScheme.Red    -> Color(0xFFC62828)
    TrailSenseColorScheme.Purple -> Color(0xFF6A1B9A)
    TrailSenseColorScheme.Teal   -> Color(0xFF00897B)
    TrailSenseColorScheme.Dynamic -> Color(0xFF6750A4)
}

/**
 * Expressive shape scale used by non-Android targets and previews.
 */
val TrailSenseExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Type scale with weight-based hierarchy. Material's default tracking is retained so long
 * localized labels remain readable and predictable in compact layouts.
 */
val TrailSenseExpressiveTypography: Typography = with(Typography()) {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.SemiBold),
        displayMedium = displayMedium.copy(fontWeight = FontWeight.SemiBold),
        displaySmall = displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = labelSmall.copy(fontWeight = FontWeight.Medium)
    )
}

/**
 * Builds the Trail Sense [ColorScheme] from a brand seed or AMOLED-tuned dark palette.
 */
@Composable
fun rememberTrailSenseColorScheme(
    colorScheme: TrailSenseColorScheme = TrailSenseColorScheme.Orange,
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false
): ColorScheme = rememberDynamicColorScheme(
    seedColor = colorScheme.toSeedColor(),
    isDark = darkTheme,
    isAmoled = isAmoled,
    style = PaletteStyle.Expressive,
    specVersion = ColorSpec.SpecVersion.SPEC_2025
)

/**
 * Multiplatform Trail Sense theme. Android delegates here after resolving platform dynamic
 * color, keeping component styling identical across targets.
 *
 * @param colorScheme Which accent / dynamic mode to use. Defaults to a warm, nature-friendly Orange.
 * @param darkTheme Whether to use dark mode. Defaults to system setting.
 * @param isAmoled Whether to use pure black surfaces (for AMOLED / "black" theme).
 * @param overrideColorScheme A fully-resolved color scheme that takes precedence over [colorScheme].
 * @param content Your UI content.
 */
@Composable
fun TrailSenseTheme(
    colorScheme: TrailSenseColorScheme = TrailSenseColorScheme.Orange,
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false,
    overrideColorScheme: ColorScheme? = null,
    content: @Composable () -> Unit
) {
    val resolvedScheme = overrideColorScheme
        ?: rememberTrailSenseColorScheme(colorScheme, darkTheme, isAmoled)

    MaterialTheme(
        colorScheme = resolvedScheme,
        typography = TrailSenseExpressiveTypography,
        shapes = TrailSenseExpressiveShapes,
        content = content
    )
}
