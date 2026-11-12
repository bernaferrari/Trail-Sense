package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIScreen

// iOS doesn't report physical density; iPhones lay out about 163 points per inch, and the ruler's calibration
// setting corrects the rest.
private const val POINTS_PER_INCH = 163f

@Composable
actual fun rememberScreenYDpi(): Float {
    return remember { UIScreen.mainScreen.scale.toFloat() * POINTS_PER_INCH }
}
