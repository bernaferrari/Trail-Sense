package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
object TrailSenseAnimation {
    val quickSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )

    val standardSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val fastFade = tween<Float>(durationMillis = TrailSenseMotion.short)
    val mediumFade = tween<Float>(durationMillis = TrailSenseMotion.medium)
}
