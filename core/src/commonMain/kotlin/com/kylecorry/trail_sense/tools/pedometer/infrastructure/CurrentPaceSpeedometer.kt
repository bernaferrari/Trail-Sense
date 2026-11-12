package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import kotlin.time.Clock

import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.core.sensors.Speedometer
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.andromeda.sense.pedometer.Pedometer
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.shared.ZERO_SPEED
import com.kylecorry.trail_sense.tools.pedometer.domain.IPaceCalculator
import kotlin.time.Instant

class CurrentPaceSpeedometer(
    private val pedometer: Pedometer,
    private val paceCalculator: IPaceCalculator
) : SharedSensor(), Speedometer {

    private val timer = CoroutineTimer {
        updateSpeed()
    }

    override var hasValidReading: Boolean = false
        private set
    override var speed: Speed = ZERO_SPEED
        private set

    private var lastSteps = 0
    private var lastTime = Instant.DISTANT_PAST

    override fun startImpl() {
        lastTime = Instant.DISTANT_PAST
        lastSteps = 0
        hasValidReading = false
        speed = ZERO_SPEED
        pedometer.start(this::onPedometerUpdate)
        timer.interval(10000)
    }

    override fun stopImpl() {
        pedometer.stop(this::onPedometerUpdate)
        timer.stop()
    }

    private fun updateSpeed() {
        if (lastTime == Instant.DISTANT_PAST) {
            lastSteps = pedometer.steps
            lastTime = Clock.System.now()
            return
        }

        val steps = pedometer.steps - lastSteps
        val duration = (Clock.System.now() - lastTime)
        speed = paceCalculator.speed(steps.toLong(), duration)
        hasValidReading = true

        lastTime = Clock.System.now()
        lastSteps = pedometer.steps

        notifyListeners()
    }

    private fun onPedometerUpdate(): Boolean {
        return true
    }
}
