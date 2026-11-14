package com.kylecorry.sol.science.astronomy.eclipse

import kotlin.time.Instant

internal data class EclipseResult(
    val start: Instant, val end: Instant, val magnitude: Float, val obscuration: Float, val maximum: Instant
)
