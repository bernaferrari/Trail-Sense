@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.main.theme

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.TrailSenseColorStyle
import com.kylecorry.trail_sense.shared.views.TrailSensePalette
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

/** Bumped when the palette or color style changes, so every themed surface picks up the new colors at once. */
object ThemeChoices {
    val revision = mutableIntStateOf(0)

    fun changed() {
        revision.intValue++
    }
}

/**
 * Android entry point for the Trail Sense theme: the user's palette and color style (Settings › Theme), with the
 * wallpaper's own scheme when that palette is chosen, and true black for the black and night themes. Every screen,
 * onboarding included, goes through here so the app never changes color between screens.
 */
@Composable
fun TrailSenseAppTheme(
    darkTheme: Boolean,
    isAmoled: Boolean,
    palette: TrailSensePalette? = null,
    style: TrailSenseColorStyle? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    // Read so a new palette or style (Settings › Theme) recolors the app in place, without restarting it.
    ThemeChoices.revision.intValue
    val chosenPalette = palette ?: prefs.palette
    val chosenStyle = style ?: prefs.colorStyle
    val wallpaperScheme = if (
        chosenPalette == TrailSensePalette.Wallpaper && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    ) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }

    TrailSenseTheme(
        palette = chosenPalette,
        style = chosenStyle,
        darkTheme = darkTheme,
        black = isAmoled,
        wallpaperScheme = wallpaperScheme
    ) {
        // UiAutomator looks up Compose testTags as resource ids.
        Box(
            modifier = Modifier.semantics { testTagsAsResourceId = true },
            propagateMinConstraints = true
        ) {
            content()
        }
    }
}
