package com.kylecorry.sol.science.astronomy.eclipse

import kotlin.time.Duration
import kotlin.time.Instant

data class Eclipse(
    val start: Instant,
    val end: Instant,
    val magnitude: Float,
    val obscuration: Float,
    val maximum: Instant = start + (end - start) / 2
) {
    val duration: Duration = end - start
}
