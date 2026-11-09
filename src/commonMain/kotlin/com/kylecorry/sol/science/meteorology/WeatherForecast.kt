package com.kylecorry.sol.science.meteorology

import kotlin.time.Instant

public data class WeatherForecast(
    val time: Instant?,
    val conditions: List<WeatherCondition>,
    val front: WeatherFront? = null,
    val system: PressureSystem? = null,
    val tendency: PressureTendency? = null
)
