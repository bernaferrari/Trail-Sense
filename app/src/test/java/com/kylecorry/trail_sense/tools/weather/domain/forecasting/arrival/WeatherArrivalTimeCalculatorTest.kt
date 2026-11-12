package com.kylecorry.trail_sense.tools.weather.domain.forecasting.arrival

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.hours
import com.kylecorry.sol.science.meteorology.PressureCharacteristic
import com.kylecorry.sol.science.meteorology.PressureTendency
import com.kylecorry.sol.science.meteorology.WeatherCondition
import com.kylecorry.sol.science.meteorology.WeatherForecast
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Clock
import kotlin.time.Instant

internal class WeatherArrivalTimeCalculatorTest {

    @Test
    fun usesTheForecastedTimeWhenItIsInTheFuture() {
        val arrival = calculator().getArrivalTime(
            listOf(forecast(time = now.plus(2.hours))),
            emptyList()
        )

        assertEquals(now.plus(2.hours), arrival?.time)
        assertTrue(arrival!!.isExact)
    }

    @Test
    fun roundsTheForecastedTimeToTheNearest15Minutes() {
        val arrival = calculator().getArrivalTime(
            listOf(forecast(time = now.plus(38.minutes))),
            emptyList()
        )

        // 12:38 rounds up to 12:45
        assertEquals(now.plus(45.minutes), arrival?.time)
        assertTrue(arrival!!.isExact)
    }

    @Test
    fun returnsTheForecastedTimeUnroundedWhenItIsInThePast() {
        val past = now.minus(38.minutes)

        val arrival = calculator().getArrivalTime(listOf(forecast(time = past)), emptyList())

        assertEquals(past, arrival?.time)
        assertTrue(arrival!!.isExact)
    }

    @Test
    fun returnsNullWhenThereIsNoForecastedTimeAndNoConditions() {
        val arrival = calculator().getArrivalTime(
            listOf(forecast(time = null, conditions = emptyList())),
            emptyList()
        )

        assertNull(arrival)
    }

    @Test
    fun stormsArriveVerySoon() {
        val arrival = calculator().getArrivalTime(
            listOf(
                forecast(
                    time = null,
                    conditions = listOf(WeatherCondition.Storm),
                    tendency = PressureTendency(PressureCharacteristic.Steady, 0f)
                )
            ),
            emptyList()
        )

        assertEquals(now.plus(1.hours), arrival?.time)
        assertFalse(arrival!!.isExact)
    }

    @Test
    fun rapidTendencyArrivesVerySoon() {
        val arrival = calculator().getArrivalTime(
            listOf(
                forecast(
                    time = null,
                    conditions = listOf(WeatherCondition.Rain),
                    tendency = PressureTendency(PressureCharacteristic.FallingFast, -1f)
                )
            ),
            emptyList()
        )

        assertEquals(now.plus(1.hours), arrival?.time)
        assertFalse(arrival!!.isExact)
    }

    @Test
    fun slowlyChangingTendencyArrivesSoon() {
        val arrival = calculator().getArrivalTime(
            listOf(
                forecast(
                    time = null,
                    conditions = listOf(WeatherCondition.Rain),
                    tendency = PressureTendency(PressureCharacteristic.Falling, -0.5f)
                )
            ),
            emptyList()
        )

        assertEquals(now.plus(2.hours), arrival?.time)
        assertFalse(arrival!!.isExact)
    }

    @Test
    fun steadyTendencyArrivesLater() {
        val arrival = calculator().getArrivalTime(
            listOf(
                forecast(
                    time = null,
                    conditions = listOf(WeatherCondition.Rain),
                    tendency = PressureTendency(PressureCharacteristic.Steady, 0f)
                )
            ),
            emptyList()
        )

        assertEquals(now.plus(8.hours), arrival?.time)
        assertFalse(arrival!!.isExact)
    }

    @Test
    fun aMissingTendencyIsTreatedAsSteady() {
        val arrival = calculator().getArrivalTime(
            listOf(forecast(time = null, conditions = listOf(WeatherCondition.Rain))),
            emptyList()
        )

        assertEquals(now.plus(8.hours), arrival?.time)
    }

    @Test
    fun onlyTheFirstForecastIsUsed() {
        val arrival = calculator().getArrivalTime(
            listOf(
                forecast(time = null, conditions = emptyList()),
                forecast(time = now.plus(2.hours))
            ),
            emptyList()
        )

        assertNull(arrival)
    }

    private fun calculator(): WeatherArrivalTimeCalculator {
        return WeatherArrivalTimeCalculator(fixedClock(now))
    }

    private fun forecast(
        time: Instant?,
        conditions: List<WeatherCondition> = listOf(WeatherCondition.Rain),
        tendency: PressureTendency? = null
    ): WeatherForecast {
        return WeatherForecast(time, conditions, tendency = tendency)
    }

    companion object {
        // 2024-01-01T12:00:00Z
        private val now = Instant.fromEpochSeconds(1704110400)
    }

    private fun fixedClock(time: Instant): Clock = object : Clock {
        override fun now(): Instant = time
    }
}
