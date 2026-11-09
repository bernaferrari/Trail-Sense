package com.kylecorry.trail_sense.tools.astronomy.domain

import com.kylecorry.sol.science.astronomy.moon.MoonTruePhase
import com.kylecorry.sol.units.Bearing
import kotlin.time.Instant

data class MoonDetails(
    val rise: Instant?,
    val set: Instant?,
    val transit: Instant?,
    val isUp: Boolean,
    val phase: MoonTruePhase,
    val phaseAngle: Float,
    val illumination: Float,
    val altitude: Float,
    val azimuth: Bearing,
    val tilt: MoonTilt
)
