package com.kylecorry.andromeda.core.time

import kotlin.time.Clock

class Throttle(private val maxTimeMs: Long) {

    private var lastTime = 0L

    fun isThrottled(): Boolean {
        val throttled = Clock.System.now().toEpochMilliseconds() - lastTime < maxTimeMs
        if (!throttled) {
            lastTime = Clock.System.now().toEpochMilliseconds()
        }
        return throttled
    }

}