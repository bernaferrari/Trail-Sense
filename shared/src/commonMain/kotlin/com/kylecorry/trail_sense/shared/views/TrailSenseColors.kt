package com.kylecorry.trail_sense.shared.views

import androidx.compose.material3.ColorScheme
import kotlin.concurrent.Volatile

/**
 * The color scheme the app's root theme is drawing with, for drawing code that runs outside composition (canvas
 * layers, map sources, list item producers). Composables read MaterialTheme.colorScheme instead.
 *
 * The root theme sets it each time it composes, so it follows the person's palette, style and light or dark choice.
 */
object TrailSenseColors {
    @Volatile
    var current: ColorScheme = trailSenseColorScheme(
        TrailSensePalette.Default,
        TrailSenseColorStyle.Tonal,
        darkTheme = false,
        black = false,
        wallpaperScheme = null
    )
}
