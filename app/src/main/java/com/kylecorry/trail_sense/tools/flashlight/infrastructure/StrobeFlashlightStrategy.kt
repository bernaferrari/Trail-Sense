package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import kotlin.time.Duration
import com.kylecorry.andromeda.core.time.interval

import com.kylecorry.luna.time.CoroutineTimer

class StrobeFlashlightStrategy(
    private val flashlight: FlashlightSubsystem,
    private val interval: Duration
) : IFlashlightStrategy {

    private var on = false
    private val timer = CoroutineTimer {
        if (on) {
            flashlight.turnOff()
        } else {
            flashlight.turnOn()
        }

        on = !on
    }

    override fun start() {
        timer.interval(interval)
    }

    override fun stop() {
        timer.stop()
        flashlight.turnOff()
    }
}
