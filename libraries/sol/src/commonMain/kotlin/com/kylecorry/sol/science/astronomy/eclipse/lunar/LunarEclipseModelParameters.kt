package com.kylecorry.sol.science.astronomy.eclipse.lunar

import kotlin.time.Instant

internal data class LunarEclipseModelParameters(
    val maximum: Instant,
    val minDistanceFromCenter: Double,
    val umbralConeRadius: Double,
    val n: Double
)
