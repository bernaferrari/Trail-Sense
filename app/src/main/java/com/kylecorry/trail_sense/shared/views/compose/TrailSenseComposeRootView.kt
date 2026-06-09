package com.kylecorry.trail_sense.shared.views.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

fun TrailSenseComposeRootView(
    context: Context,
    compositionStrategy: ViewCompositionStrategy = ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
    content: @Composable () -> Unit
): ComposeView {
    return ComposeView(context).apply {
        setViewCompositionStrategy(compositionStrategy)
        setContent {
            TrailSenseTheme {
                content()
            }
        }
    }
}
