package com.kylecorry.trail_sense.tools.weather.domain

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class CanSendDailyForecastTest {

    @Test
    fun isSatisfiedBy() {
        // start, now, whether the forecast may be sent (a three hour window that can cross midnight)
        val cases = listOf(
            Triple(LocalTime(0, 0), LocalTime(0, 0), true),
            Triple(LocalTime(0, 0), LocalTime(3, 0), true),
            Triple(LocalTime(0, 0), LocalTime(3, 1), false),
            Triple(LocalTime(12, 0), LocalTime(12, 0), true),
            Triple(LocalTime(12, 0), LocalTime(15, 0), true),
            Triple(LocalTime(12, 0), LocalTime(16, 0), false),
            Triple(LocalTime(22, 0), LocalTime(22, 0), true),
            Triple(LocalTime(22, 0), LocalTime(1, 0), true),
            Triple(LocalTime(22, 0), LocalTime(2, 0), false),
            Triple(LocalTime(0, 30), LocalTime(3, 30), true),
            Triple(LocalTime(0, 30), LocalTime(3, 31), false)
        )
        for ((start, now, expected) in cases) {
            assertEquals(expected, CanSendDailyForecast(start).isSatisfiedBy(now), "start $start, now $now")
        }
    }
}
