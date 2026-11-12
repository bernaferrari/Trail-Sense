package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberScreenYDpi(): Float {
    val context = LocalContext.current
    return remember(context) { context.resources.displayMetrics.ydpi }
}
