package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.runtime.Composable

/** The screen's physical pixels per inch along its vertical axis, used to draw true-to-size scales. */
@Composable
expect fun rememberScreenYDpi(): Float
