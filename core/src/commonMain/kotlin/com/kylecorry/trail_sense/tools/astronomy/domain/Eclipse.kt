package com.kylecorry.trail_sense.tools.astronomy.domain

import com.kylecorry.sol.math.MathExtensions.roundPlaces
import kotlin.time.Duration
import kotlin.time.Instant

data class Eclipse(
    val start: Instant,
    val end: Instant,
    val peak: Instant,
    val magnitude: Float,
    val obscuration: Float,
    val peakAltitude: Float
) {
    val isTotal: Boolean = magnitude.roundPlaces(2) >= 1f
    val duration: Duration = (end - start)
}
