package com.kylecorry.sol.science.astronomy.eclipse.solar

import kotlin.time.Instant

internal data class SolarEclipseModelParameters(
    val maximum: Instant,
    val minDistanceFromCenter: Double,
    val umbralConeRadius: Double
)
