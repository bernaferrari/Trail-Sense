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
                isAmoled = isAmoled,
                useDynamicColors = preferences.useDynamicColors
            ) {
                content()
            }
        }
    }
}
