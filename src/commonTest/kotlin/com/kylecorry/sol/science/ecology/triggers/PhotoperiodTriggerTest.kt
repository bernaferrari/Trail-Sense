package com.kylecorry.sol.science.ecology.triggers

import kotlin.test.Test
import kotlin.test.assertEquals

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.ecology.LifecycleEventFactors
import com.kylecorry.sol.units.Temperature
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.LocalDate

class PhotoperiodTriggerTest {

    @Test
    fun aboveTriggeredWhenDayLengthIsAboveThreshold() {
        aboveTriggeredWhenDayLengthIsAboveThreshold(12L, false)
        aboveTriggeredWhenDayLengthIsAboveThreshold(13L, true)
        aboveTriggeredWhenDayLengthIsAboveThreshold(11L, false)
    }

    private fun aboveTriggeredWhenDayLengthIsAboveThreshold(dayLengthHours: Long, isTriggered: Boolean) {
        val trigger = PhotoperiodTrigger(12.hours, above = true)
        val factors = factors(dayLengthHours)
        assertEquals(isTriggered, trigger.isTriggered(factors))
    }

    @Test
    fun belowTriggeredWhenDayLengthIsBelowThreshold() {
        belowTriggeredWhenDayLengthIsBelowThreshold(12L, false)
        belowTriggeredWhenDayLengthIsBelowThreshold(11L, true)
        belowTriggeredWhenDayLengthIsBelowThreshold(13L, false)
    }

    private fun belowTriggeredWhenDayLengthIsBelowThreshold(dayLengthHours: Long, isTriggered: Boolean) {
        val trigger = PhotoperiodTrigger(12.hours, above = false)
        val factors = factors(dayLengthHours)
        assertEquals(isTriggered, trigger.isTriggered(factors))
    }

    private fun factors(dayLengthHours: Long): LifecycleEventFactors {
        return LifecycleEventFactors(
            lengthOfDay = dayLengthHours.hours,
            temperature = Range(Temperature.celsius(0f), Temperature.celsius(0f)),
            date = LocalDate(2025, 1, 1)
        )
    }
}
