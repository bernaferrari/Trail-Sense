package com.kylecorry.sol.science.ecology.triggers

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.ecology.LifecycleEventFactors
import com.kylecorry.sol.units.Temperature
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.LocalDate

class DurationTriggerTest {

    @Test
    fun triggersAfterDurationFromBaseTriggerDate() {
        val baseTrigger = DateThresholdTrigger(LocalDate(2024, 1, 2))
        val trigger = DurationTrigger(baseTrigger, 2.days)

        assertFalse(trigger.isTriggered(factors(LocalDate(2024, 1, 1))))
        assertFalse(trigger.isTriggered(factors(LocalDate(2024, 1, 2))))
        assertFalse(trigger.isTriggered(factors(LocalDate(2024, 1, 3))))
        assertTrue(trigger.isTriggered(factors(LocalDate(2024, 1, 4))))
    }

    @Test
    fun resetWhenBaseNotTriggeredClearsPendingTriggerDate() {
        val baseTrigger = OneDayTrigger(LocalDate(2024, 1, 2))
        val trigger = DurationTrigger(baseTrigger, 2.days, resetWhenBaseNotTriggered = true)

        assertFalse(trigger.isTriggered(factors(LocalDate(2024, 1, 2))))
        assertFalse(trigger.isTriggered(factors(LocalDate(2024, 1, 3))))
        assertFalse(trigger.isTriggered(factors(LocalDate(2024, 1, 4))))
    }

    @Test
    fun resetCallsResetOnBaseTrigger() {
        val baseTrigger = ResetTrackingTrigger()
        val trigger = DurationTrigger(baseTrigger, 1.days)

        trigger.reset()

        assertEquals(1, baseTrigger.resetCount)
    }

    private fun factors(date: LocalDate): LifecycleEventFactors {
        return LifecycleEventFactors(
            lengthOfDay = 12.hours,
            temperature = Range(Temperature.celsius(0f), Temperature.celsius(0f)),
            date = date
        )
    }

    private class DateThresholdTrigger(private val startDate: LocalDate) : LifecycleEventTrigger {
        override fun isTriggered(factors: LifecycleEventFactors): Boolean {
            return factors.date >= startDate
        }
    }

    private class OneDayTrigger(private val triggerDate: LocalDate) : LifecycleEventTrigger {
        override fun isTriggered(factors: LifecycleEventFactors): Boolean {
            return factors.date == triggerDate
        }
    }

    private class ResetTrackingTrigger : LifecycleEventTrigger {
        var resetCount = 0
            private set

        override fun isTriggered(factors: LifecycleEventFactors): Boolean = false

        override fun reset() {
            resetCount++
        }
    }
}
