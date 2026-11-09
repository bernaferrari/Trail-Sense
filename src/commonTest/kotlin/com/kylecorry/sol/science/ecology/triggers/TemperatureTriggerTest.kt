package com.kylecorry.sol.science.ecology.triggers

import kotlin.test.Test
import kotlin.test.assertEquals

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.ecology.LifecycleEventFactors
import com.kylecorry.sol.units.Temperature
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.datetime.LocalDate

class TemperatureTriggerTest {

    @Test
    fun aboveTriggeredWhenTemperatureIsAboveThreshold() {
        aboveTriggeredWhenTemperatureIsAboveThreshold(10f, false)
        aboveTriggeredWhenTemperatureIsAboveThreshold(11f, true)
        aboveTriggeredWhenTemperatureIsAboveThreshold(9f, false)
    }

    private fun aboveTriggeredWhenTemperatureIsAboveThreshold(temperature: Float, isTriggered: Boolean) {
        val trigger = TemperatureTrigger(Temperature.celsius(10f), above = true)
        val factors = factors(low = 0f, high = temperature)
        assertEquals(isTriggered, trigger.isTriggered(factors))
    }

    @Test
    fun belowTriggeredWhenTemperatureIsBelowThreshold() {
        belowTriggeredWhenTemperatureIsBelowThreshold(0f, false)
        belowTriggeredWhenTemperatureIsBelowThreshold(-1f, true)
        belowTriggeredWhenTemperatureIsBelowThreshold(1f, false)
    }

    private fun belowTriggeredWhenTemperatureIsBelowThreshold(temperature: Float, isTriggered: Boolean) {
        val trigger = TemperatureTrigger(Temperature.celsius(0f), above = false)
        val factors = factors(low = temperature, high = 0f)
        assertEquals(isTriggered, trigger.isTriggered(factors))
    }

    @Test
    fun triggeredUsesConfiguredTemperatureType() {
        triggeredUsesConfiguredTemperatureType(true, TemperatureTriggerType.High, true)
        triggeredUsesConfiguredTemperatureType(true, TemperatureTriggerType.Low, false)
        triggeredUsesConfiguredTemperatureType(true, TemperatureTriggerType.Average, false)
        triggeredUsesConfiguredTemperatureType(false, TemperatureTriggerType.High, false)
        triggeredUsesConfiguredTemperatureType(false, TemperatureTriggerType.Low, true)
        triggeredUsesConfiguredTemperatureType(false, TemperatureTriggerType.Average, false)
    }

    private fun triggeredUsesConfiguredTemperatureType(
        above: Boolean,
        triggerType: TemperatureTriggerType,
        isTriggered: Boolean
    ) {
        val trigger = TemperatureTrigger(Temperature.celsius(10f), above, triggerType)
        val factors = factors(low = 0f, high = 20f)
        assertEquals(isTriggered, trigger.isTriggered(factors))
    }

    private fun factors(low: Float, high: Float): LifecycleEventFactors {
        return LifecycleEventFactors(
            lengthOfDay = 12.hours,
            temperature = Range(Temperature.celsius(low), Temperature.celsius(high)),
            date = LocalDate(2025, 1, 1)
        )
    }
}
