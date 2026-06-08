package com.kylecorry.trail_sense.shared.views.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

class TrailSenseComposeRootView(
    context: Context,
    private val content: @Composable () -> Unit
) : AbstractComposeView(context) {

    init {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    @Composable
    override fun Content() {
        TrailSenseTheme {
            content()
        }
    }
}
