package com.kylecorry.trail_sense.tools.astronomy.domain

import com.kylecorry.sol.units.Bearing
import kotlin.time.Instant

data class SunDetails(
    val rise: Instant?,
    val set: Instant?,
    val transit: Instant?,
    val isUp: Boolean,
    val nextRise: Instant?,
    val nextSet: Instant?,
    val altitude: Float,
    val azimuth: Bearing
) {
    val nextTransition: AstronomyTransition?
        get() {
            if (nextRise == null && nextSet == null) {
                return null
            }
            if (nextRise == null) {
                return AstronomyTransition.Set
            }
            if (nextSet == null) {
                return AstronomyTransition.Rise
            }
            return if (nextRise < nextSet) AstronomyTransition.Rise else AstronomyTransition.Set
        }
}
