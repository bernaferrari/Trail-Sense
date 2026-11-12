package com.kylecorry.trail_sense.shared.navigation

import androidx.compose.runtime.staticCompositionLocalOf

/** The app's navigator, for any screen drawn inside [TrailSenseNavDisplay]. */
val LocalNavigator = staticCompositionLocalOf<TrailSenseNavigator> {
    error("LocalNavigator is only available inside TrailSenseNavDisplay")
}
