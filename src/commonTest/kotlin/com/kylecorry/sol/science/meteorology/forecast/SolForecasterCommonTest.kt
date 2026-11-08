package com.kylecorry.sol.science.meteorology.forecast

import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureSystem
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.shared.Season
import com.kylecorry.sol.science.meteorology.forecast.sol.SolForecaster
import com.kylecorry.sol.science.meteorology.observation.WeatherObservation
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Pressure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class SolForecasterCommonTest {
    @Test
    fun zambrettiForecastsSteadyPressure() {
        val time = Instant.parse("2025-01-01T12:00:00Z")
        val observations = listOf(
            WeatherObservation.Pressure(time - 3.hours, Pressure.hpa(1025f)),
            WeatherObservation.Pressure(time, Pressure.hpa(1025f))
        )
        val forecast = ZambrettiForecaster { _, _ -> Season.Winter }
            .forecast(observations, null, time, 0.5f, 2f, Coordinate.zero)
        assertEquals(listOf(WeatherCondition.Clear), forecast.first().conditions)
    }

    @Test
    fun forecastsSteadyHighPressure() {
        val time = Instant.parse("2025-01-01T12:00:00Z")
        val observations = listOf(
            WeatherObservation.Pressure(time - 3.hours, Pressure.hpa(1030f)),
            WeatherObservation.Pressure(time, Pressure.hpa(1030f))
        )
        val forecast = SolForecaster.forecast(observations, null, time, 0.5f, 2f, Coordinate.zero)
        assertEquals(PressureSystem.High, forecast.first().system)
        assertEquals(PressureCharacteristic.Steady, forecast.first().tendency?.characteristic)
    }
}
