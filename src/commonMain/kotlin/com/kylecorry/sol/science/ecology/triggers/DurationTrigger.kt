package com.kylecorry.sol.science.ecology.triggers

import com.kylecorry.sol.science.ecology.LifecycleEventFactors
import kotlin.time.Duration
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

class DurationTrigger(
    private val baseTrigger: LifecycleEventTrigger,
    private val duration: Duration,
    private val resetWhenBaseNotTriggered: Boolean = false
) : LifecycleEventTrigger {

    private var triggerDate: LocalDate? = null

    override fun isTriggered(factors: LifecycleEventFactors): Boolean {
        val isBaseTriggered = baseTrigger.isTriggered(factors)
        if (isBaseTriggered && triggerDate == null) {
            triggerDate = factors.date.plus(duration.inWholeDays.toInt(), DateTimeUnit.DAY)
        }
        if (!isBaseTriggered && resetWhenBaseNotTriggered) {
            triggerDate = null
        }

        val currentTriggerDate = triggerDate
        return currentTriggerDate != null && factors.date >= currentTriggerDate
    }

    override fun reset() {
        triggerDate = null
        baseTrigger.reset()
    }
}
