package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIAccessibilityIsReduceMotionEnabled

@Composable
actual fun rememberSystemAnimationsEnabled(): Boolean {
    return remember { !UIAccessibilityIsReduceMotionEnabled() }
}
