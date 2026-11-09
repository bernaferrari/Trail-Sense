package com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.calculators

import com.kylecorry.sol.units.Temperature
import kotlin.time.Instant

internal interface ITemperatureCalculator {
    suspend fun calculate(time: Instant): Temperature
}
