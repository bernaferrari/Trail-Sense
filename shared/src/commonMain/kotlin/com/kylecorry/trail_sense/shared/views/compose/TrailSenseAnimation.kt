package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp

object TrailSenseAnimation {
    val quickSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val standardSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val fastFade = tween<Float>(durationMillis = TrailSenseMotion.short)
    val mediumFade = tween<Float>(durationMillis = TrailSenseMotion.medium)
}