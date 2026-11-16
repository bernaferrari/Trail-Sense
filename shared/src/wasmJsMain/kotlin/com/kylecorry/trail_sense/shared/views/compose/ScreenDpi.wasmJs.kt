package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

@Composable
actual fun rememberScreenYDpi(): Float {
    return remember {
        (window.devicePixelRatio.toFloat().takeIf { it > 0f } ?: 1f) * 96f
    }
}
