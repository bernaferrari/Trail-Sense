package com.kylecorry.trail_sense.shared.hooks

import kotlinx.atomicfu.locks.synchronized

import kotlinx.atomicfu.locks.SynchronizedObject

import kotlin.time.Duration
import kotlin.time.Instant

class FrequencyHookTrigger {

    private var lastTime: Instant? = null
    private val lock = SynchronizedObject()
    private var lastReturnValue = false

    fun getValue(time: Instant, threshold: Duration): Boolean {
        return synchronized(lock) {
            val previous = lastTime
            if (previous == null || (time - previous) >= threshold) {
                lastTime = time
                lastReturnValue = !lastReturnValue
            }
            lastReturnValue
        }
    }
}
