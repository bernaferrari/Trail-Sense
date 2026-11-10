package com.kylecorry.trail_sense.tools.climate.infrastructure.temperatures.calculators

import com.kylecorry.sol.math.arithmetic.Arithmetic.square

import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import kotlin.time.Instant
import com.kylecorry.sol.time.Time.hoursBetween

class QuadraticTemperatureCalculator(
    private val low: Reading<Temperature>,
    high: Reading<Temperature>
) : ITemperatureCalculator {

    private val b = low.value.celsius().value
    private val a = (high.value.celsius().value - b) / square(getX(high.time))

    override suspend fun calculate(time: Instant): Temperature {
        val x = getX(time)
        return Temperature.celsius(a * square(x) + b)
    }

    private fun getX(time: Instant): Float {
        return hoursBetween(low.time, time)
    }
}
