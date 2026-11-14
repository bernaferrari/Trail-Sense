package com.kylecorry.sol.science.meteorology.forecast

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.meteorology.PressureModel
import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureSystem
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Temperature
import kotlin.time.Duration.Companion.hours

internal object ForecastHelper {

    val overcastClouds = setOf(
        CloudGenus.Stratus,
        CloudGenus.Nimbostratus,
        CloudGenus.Stratocumulus,
        CloudGenus.Altostratus
    )

    fun addSecondaryConditions(
        conditions: List<WeatherCondition>,
        dailyTemperatureRange: Range<Temperature>? = null,
    ): List<WeatherCondition> {
        val mutableConditions = conditions.toMutableList()

        // Storms should always be paired with precipitation and wind
        if (mutableConditions.contains(WeatherCondition.Storm)) {
            mutableConditions.add(WeatherCondition.Precipitation)
            mutableConditions.add(WeatherCondition.Wind)
        }

        // Precipitation should always be paired with overcast
        if (mutableConditions.contains(WeatherCondition.Precipitation)) {
            mutableConditions.add(WeatherCondition.Overcast)

            // Try to infer the precipitation type
            val precipitationType = getPrecipitationType(dailyTemperatureRange)
            if (precipitationType != null) {
                mutableConditions.add(precipitationType)
            }
        }

        return mutableConditions.distinct()
    }

    fun getPressureSystem(pressure: Pressure): PressureSystem? {
        return if (PressureModel.isHigh(pressure)) {
            PressureSystem.High
        } else if (PressureModel.isLow(pressure)) {
            PressureSystem.Low
        } else {
            null
        }
    }

    fun getTendency(
        pressures: List<Reading<Pressure>>,
        threshold: Float
    ): PressureTendency {
        if (pressures.size < 2) {
            return PressureTendency(PressureCharacteristic.Steady, 0f)
        }

        val pressure = pressures.last()

        val targetTime = pressure.time - 3.hours
        val lastPressure = pressures.minBy {
            if (it == pressure) {
                Long.MAX_VALUE
            } else {
                (targetTime - it.time).absoluteValue.inWholeSeconds
            }
        }
        val seconds = pressure.time.epochSeconds - lastPressure.time.epochSeconds -
            if (pressure.time.nanosecondsOfSecond < lastPressure.time.nanosecondsOfSecond) 1 else 0
        return PressureModel.tendency(lastPressure.value, pressure.value, seconds, threshold)
    }

    private fun getPrecipitationType(temperatures: Range<Temperature>?): WeatherCondition? {
        temperatures ?: return null

        if (temperatures.start.celsius().value > 0) {
            return WeatherCondition.Rain
        }

        if (temperatures.end.celsius().value <= 0) {
            return WeatherCondition.Snow
        }

        return null
    }
}
