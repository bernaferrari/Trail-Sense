package com.kylecorry.trail_sense.shared.views.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

class TrailSenseComposeRootView(
    context: Context,
    compositionStrategy: ViewCompositionStrategy = ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
    private val content: @Composable () -> Unit
) : AbstractComposeView(context) {

    init {
        setViewCompositionStrategy(compositionStrategy)
    }

    @Composable
    override fun Content() {
        TrailSenseTheme {
            content()
        }
    }
}
