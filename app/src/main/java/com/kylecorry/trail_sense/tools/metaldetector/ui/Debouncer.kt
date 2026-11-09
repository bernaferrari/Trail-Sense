package com.kylecorry.trail_sense.tools.metaldetector.ui

import kotlin.time.Duration

class Debouncer(var debounceTime: Duration, initialValue: Boolean = false) {


    val value: Boolean
        get() {
            if (lastValue == lastAttemptedValue || lastAttemptedTime - lastTime > debounceTime.inWholeMilliseconds) {
                lastValue = lastAttemptedValue
                lastTime = lastAttemptedTime
            }
            return lastValue
        }

    private var lastValue = initialValue
    private var lastTime = System.currentTimeMillis()

    private var lastAttemptedValue = initialValue
    private var lastAttemptedTime = System.currentTimeMillis()

    fun update(newValue: Boolean) {
        lastAttemptedValue = newValue
        lastAttemptedTime = System.currentTimeMillis()
    }

}
