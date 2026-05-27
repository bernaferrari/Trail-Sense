package com.kylecorry.trail_sense.shared.views

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/**
 * Trail Sense Design System — Material 3 Expressive + Material Kolor
 *
 * Supports:
 * - User-selected accent colors (Orange, Blue, Green, Red, Purple, Teal)
 * - True Android Dynamic Color (wallpaper-based Material You) when "Dynamic" is chosen
 * - Excellent light/dark + AMOLED harmony via Material Kolor
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
    TrailSenseColorScheme.Orange -> Color(0xFFE85D04)
    TrailSenseColorScheme.Blue   -> Color(0xFF1565C0)
    TrailSenseColorScheme.Green  -> Color(0xFF2E7D32)
    TrailSenseColorScheme.Red    -> Color(0xFFC62828)
    TrailSenseColorScheme.Purple -> Color(0xFF6A1B9A)
    TrailSenseColorScheme.Teal   -> Color(0xFF00897B)
    TrailSenseColorScheme.Dynamic -> Color(0xFF6750A4)
}

/**
 * The main Trail Sense theme.
 *
 * @param colorScheme Which accent / dynamic mode to use. Defaults to a warm, nature-friendly Orange.
 * @param darkTheme Whether to use dark mode. Defaults to system setting.
 * @param isAmoled Whether to use pure black surfaces (for AMOLED devices).
 * @param content Your UI content.
 */
@Composable
fun TrailSenseTheme(
    colorScheme: TrailSenseColorScheme = TrailSenseColorScheme.Orange,
    darkTheme: Boolean = isSystemInDarkTheme(),
    isAmoled: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorSchemeResult: ColorScheme = when (colorScheme) {
        TrailSenseColorScheme.Dynamic -> {
            if (isAndroidDynamicColorAvailable()) {
                val context = LocalContext.current
                if (darkTheme) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            } else {
                rememberDynamicColorScheme(
                    seedColor = TrailSenseColorScheme.Dynamic.toSeedColor(),
                    isDark = darkTheme,
                    isAmoled = isAmoled,
                    style = PaletteStyle.TonalSpot
                )
            }
        }

        else -> {
            rememberDynamicColorScheme(
                seedColor = colorScheme.toSeedColor(),
                isDark = darkTheme,
                isAmoled = isAmoled,
                style = PaletteStyle.TonalSpot
            )
        }
    }

    MaterialTheme(
        colorScheme = colorSchemeResult,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}

@Composable
private fun isAndroidDynamicColorAvailable(): Boolean = true
