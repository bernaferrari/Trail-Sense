package com.kylecorry.sol.science.astronomy.stars

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

public data class StarReading(
    val star: Star, val altitude: Float, val azimuth: Float?, val time: Instant,
    val timeZone: TimeZone = TimeZone.currentSystemDefault()
)
