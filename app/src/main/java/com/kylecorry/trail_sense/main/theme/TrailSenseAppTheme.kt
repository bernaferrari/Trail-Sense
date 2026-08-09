@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.main.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.trail_sense.shared.views.TrailSenseColorScheme
import com.kylecorry.trail_sense.shared.views.TrailSenseExpressiveShapes
import com.kylecorry.trail_sense.shared.views.TrailSenseExpressiveTypography
import com.kylecorry.trail_sense.shared.views.rememberTrailSenseColorScheme

/**
 * Android entry point for the Trail Sense theme.
 *
 * Bridges the user's real preferences — dark mode, AMOLED/black theme, and wallpaper-based
 * dynamic color — into the Material 3 color scheme. When dynamic color is enabled on Android
 * 12+, the system wallpaper palette is used; otherwise the app's warm Orange brand seed drives
 * the scheme (via Material Kolor in :shared). Public Material 3 APIs provide the expressive
 * typography, shapes, color, and component behavior on the current Compose version.
 */
@Composable
fun TrailSenseAppTheme(
    darkTheme: Boolean,
    isAmoled: Boolean,
    useDynamicColors: Boolean,
    fallbackColorScheme: TrailSenseColorScheme = TrailSenseColorScheme.Orange,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme: ColorScheme = if (
        useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        val base = if (darkTheme) {
            dynamicDarkColorScheme(context)
        } else {
            dynamicLightColorScheme(context)
        }
        // For AMOLED / pure-black dark theme, collapse the dark surfaces to true black.
        if (isAmoled && darkTheme) base.toAmoled() else base
    } else {
        rememberTrailSenseColorScheme(
            colorScheme = fallbackColorScheme,
            darkTheme = darkTheme,
            isAmoled = isAmoled
        )
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        shapes = TrailSenseExpressiveShapes,
        typography = TrailSenseExpressiveTypography,
        content = content
    )
}

private fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222)
)
