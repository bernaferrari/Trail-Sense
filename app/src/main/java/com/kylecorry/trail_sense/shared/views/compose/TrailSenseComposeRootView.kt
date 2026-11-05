package com.kylecorry.trail_sense.shared.views.compose

import android.content.Context
import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.kylecorry.trail_sense.main.theme.AppThemeMode
import com.kylecorry.trail_sense.main.theme.ThemeProvider
import com.kylecorry.trail_sense.main.theme.TrailSenseAppTheme
import com.kylecorry.trail_sense.shared.UserPreferences
import java.util.Collections
import java.util.WeakHashMap

/**
 * Compose content wrapped in a View, for the places that still need one: home-screen widgets drawing to a bitmap,
 * and callers that haven't moved to the dialogs' `body` slot yet (the dialogs draw such a view's [content] directly
 * in Compose, see [composeContentOf]).
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
        composeContents[this] = content
    }
}

private val composeContents: MutableMap<View, @Composable () -> Unit> =
    Collections.synchronizedMap(WeakHashMap())

/** The Compose content of a view made by [TrailSenseComposeRootView], or null for any other view. */
fun composeContentOf(view: View): (@Composable () -> Unit)? = composeContents[view]
