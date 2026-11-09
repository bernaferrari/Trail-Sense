package com.kylecorry.trail_sense.tools.metaldetector.ui

import kotlin.time.Clock

import kotlin.time.Duration

class Debouncer(var debounceTime: Duration, initialValue: Boolean = false) {


    val value: Boolean
        get() {
            if (lastValue == lastAttemptedValue || lastAttemptedTime - lastTime > debounceTime) {
                lastValue = lastAttemptedValue
                lastTime = lastAttemptedTime
            }
            return lastValue
        }

    private var lastValue = initialValue
    private var lastTime = Clock.System.now()

    private var lastAttemptedValue = initialValue
    private var lastAttemptedTime = Clock.System.now()

    fun update(newValue: Boolean) {
        lastAttemptedValue = newValue
        lastAttemptedTime = Clock.System.now()
    }

}
