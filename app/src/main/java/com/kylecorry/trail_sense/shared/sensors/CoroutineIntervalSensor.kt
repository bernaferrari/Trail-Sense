package com.kylecorry.trail_sense.shared.sensors

import kotlin.time.toJavaDuration

import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.luna.time.CoroutineTimer
import kotlin.time.Duration

abstract class CoroutineIntervalSensor(private val frequency: Duration) : SharedSensor() {

    private val timer = CoroutineTimer { update() }

    override val hasValidReading: Boolean
        get() = true

    override fun startImpl() {
        timer.interval(frequency.toJavaDuration())
    }

    override fun stopImpl() {
        timer.stop()
    }

    protected open suspend fun update() {
        onMain {
            notifyListeners()
        }
    }

}
