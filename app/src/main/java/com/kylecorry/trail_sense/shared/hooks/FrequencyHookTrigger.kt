package com.kylecorry.trail_sense.shared.hooks

import kotlin.time.Duration
import kotlin.time.Instant

internal class FrequencyHookTrigger {

    private var lastTime: Instant? = null
    private val lock = Any()
    private var lastReturnValue = false

    fun getValue(time: Instant, threshold: Duration): Boolean {
        synchronized(lock) {
            val previous = lastTime
            if (previous == null || (time - previous) >= threshold) {
                lastTime = time
                lastReturnValue = !lastReturnValue
                return lastReturnValue
            }

            return lastReturnValue
        }
    }
}
