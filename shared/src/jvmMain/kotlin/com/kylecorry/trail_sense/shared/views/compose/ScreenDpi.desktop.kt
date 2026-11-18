package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

@Composable
actual fun rememberScreenYDpi(): Float = 96f * LocalDensity.current.density
