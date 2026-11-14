package com.kylecorry.sol.science.ecology

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.ecology.triggers.DurationTrigger
import com.kylecorry.sol.science.ecology.triggers.LifecycleEventTrigger
import com.kylecorry.sol.units.Temperature
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class EcologyCommonTest {
    @Test
    fun durationTriggerCrossesLeapDay() {
        val trigger = DurationTrigger(object : LifecycleEventTrigger {
            override fun isTriggered(factors: LifecycleEventFactors) = true
        }, 2.days)
        fun factors(date: LocalDate) = LifecycleEventFactors(12.hours, Range(Temperature.celsius(1f), Temperature.celsius(2f)), date)
        assertFalse(trigger.isTriggered(factors(LocalDate(2024, 2, 28))))
        assertFalse(trigger.isTriggered(factors(LocalDate(2024, 2, 29))))
        assertTrue(trigger.isTriggered(factors(LocalDate(2024, 3, 1))))
    }
}
