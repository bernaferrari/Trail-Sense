package com.kylecorry.trail_sense.shared.views.compose

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.kylecorry.trail_sense.main.theme.AppThemeMode
import com.kylecorry.trail_sense.main.theme.ThemeProvider
import com.kylecorry.trail_sense.main.theme.TrailSenseAppTheme
import com.kylecorry.trail_sense.shared.UserPreferences

/**
 * Compose content wrapped in a View, only for home-screen widgets: Android draws widgets from RemoteViews, so a
 * widget renders its Compose chart into a bitmap through this view.
 */
fun TrailSenseComposeRootView(
    context: Context,
    compositionStrategy: ViewCompositionStrategy = ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
    content: @Composable () -> Unit
): ComposeView {
    val preferences = UserPreferences(context)
    val themeProvider = ThemeProvider()
    return ComposeView(context).apply {
        setViewCompositionStrategy(compositionStrategy)
        setContent {
            val darkTheme = when (themeProvider.getThemeMode()) {
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
                AppThemeMode.System -> isSystemInDarkTheme()
            }
            val isAmoled = darkTheme && preferences.theme in setOf(
                UserPreferences.Theme.Black,
                UserPreferences.Theme.Night,
                UserPreferences.Theme.SystemBlack
            )
            TrailSenseAppTheme(
                darkTheme = darkTheme,
                isAmoled = isAmoled
            ) {
                content()
            }
        }
    }
}
