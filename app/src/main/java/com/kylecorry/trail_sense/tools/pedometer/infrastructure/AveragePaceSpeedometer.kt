package com.kylecorry.trail_sense.tools.pedometer.infrastructure

import android.os.Bundle
import com.kylecorry.andromeda.core.sensors.AbstractSensor
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.units.Speed
import com.kylecorry.trail_sense.shared.ZERO_SPEED
import com.kylecorry.trail_sense.settings.infrastructure.IPedometerPreferences
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.domain.IPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AveragePaceSpeedometer(
    private val stepTrackerService: IStepTrackerService,
    private val paceCalculator: IPaceCalculator,
    private val pedometerPreferences: IPedometerPreferences
) : AbstractSensor(), ISpeedometer {

    private var scope = newScope()

    private val timer = CoroutineTimer {
        scope.launch { refresh() }
    }

    override var hasValidReading: Boolean = false
        private set

    override var speed: Speed = ZERO_SPEED
        private set

    override fun startImpl() {
        if (!scope.isActive) {
            scope = newScope()
        }
        timer.interval(10000)
        Tools.subscribe(PedometerToolRegistration.BROADCAST_STEPS_CHANGED, this::onStepsChanged)
    }

    override fun stopImpl() {
        timer.stop()
        Tools.unsubscribe(PedometerToolRegistration.BROADCAST_STEPS_CHANGED, this::onStepsChanged)
        scope.cancel()
    }

    private fun onStepsChanged(@Suppress("UNUSED_PARAMETER") data: Bundle) {
        scope.launch { refresh() }
    }

    private suspend fun refresh() {
        val stepPeriod = stepTrackerService.getOpenStepTrackingPeriod() ?: run {
            reset()
            return
        }
        val duration = stepPeriod.getDuration(pedometerPreferences.averagePaceTimeMode)
        speed = paceCalculator.speed(stepPeriod.steps, duration)
        hasValidReading = true
        notifyListeners()
    }

    private fun reset() {
        hasValidReading = false
        speed = ZERO_SPEED
    }

    private fun newScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
