package com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import com.kylecorry.andromeda.permissions.Permissions
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.luna.concurrency.BackgroundTask
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.preferences.onEachChange
import com.kylecorry.trail_sense.tools.pedometer.PedometerToolRegistration
import com.kylecorry.trail_sense.tools.pedometer.domain.StepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.StepCounterService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PedometerSubsystem private constructor(private val context: Context) : IPedometerSubsystem {

    private val sharedPrefs by lazy { PreferencesSubsystem.getInstance(context).preferences }
    private val prefs by lazy { UserPreferences(context) }
    private val stepTrackerService by lazy { getAppService<StepTrackerService>() }
    private val _steps = MutableStateFlow(0L)
    private val _distance = MutableStateFlow(calculateDistance())
    private val _state = MutableStateFlow(calculateState())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val steps: StateFlow<Long> = _steps.asStateFlow()
    override val distance: StateFlow<Distance> = _distance.asStateFlow()
    override val state: StateFlow<FeatureState> = _state.asStateFlow()

    override fun enable() {
        prefs.pedometer.isEnabled = true
        ToolBroadcasts.broadcast(PedometerToolRegistration.BROADCAST_PEDOMETER_ENABLED)
        StepCounterService.start(context)
    }

    override fun disable() {
        prefs.pedometer.isEnabled = false
        ToolBroadcasts.broadcast(PedometerToolRegistration.BROADCAST_PEDOMETER_DISABLED)
        StepCounterService.stop(context)
    }

    private val stateChangePrefKeys = listOf(
        PreferenceKeys.pedometerEnabled,
        PreferenceKeys.lowPowerMode
    )

    private val distanceChangePrefKeys = listOf(
        PreferenceKeys.strideLength
    )

    private val stepsMutex = Mutex()
    private val stepsQueue = CoroutineQueueRunner()
    private val initialPopulationTask = BackgroundTask {
        updateSteps()
    }

    init {
        initialPopulationTask.start()
        ToolBroadcasts.subscribe(PedometerToolRegistration.BROADCAST_STEPS_CHANGED) {
            BackgroundTask {
                stepsQueue.enqueue {
                    updateSteps(it)
                }
            }.start()
        }

        // Tell the rest of the app when the distance changes (a state flow only emits changes)
        scope.launch {
            distance.drop(1).collect {
                ToolBroadcasts.broadcast(PedometerToolRegistration.BROADCAST_DISTANCE_CHANGED)
            }
        }

        sharedPrefs.onEachChange {
            if (it in stateChangePrefKeys) {
                _state.value = calculateState()
            }

            if (it in distanceChangePrefKeys) {
                _distance.value = calculateDistance()
            }
        }
    }

    private suspend fun updateSteps(data: EventData? = null) = stepsMutex.withLock {
        val stepCount = data?.getLong(PedometerToolRegistration.BROADCAST_PARAM_STEPS)
            ?: stepTrackerService.getOpenStepTrackingPeriod()?.steps
            ?: 0L
        _steps.value = stepCount
        _distance.value = calculateDistance()
    }

    private fun calculateState(): FeatureState {
        return if (isDisabled()) {
            FeatureState.Unavailable
        } else if (prefs.pedometer.isEnabled) {
            FeatureState.On
        } else {
            FeatureState.Off
        }
    }

    override fun isDisabledDueToPermissions(): Boolean {
        return !Permissions.canRecognizeActivity(context) && !prefs.isLowPowerModeOn
    }

    fun recalculateState() {
        _state.value = calculateState()
    }

    private fun isDisabled(): Boolean {
        val hasPermission = Permissions.canRecognizeActivity(context)
        return !Sensors.hasSensor(
            context,
            Sensor.TYPE_STEP_COUNTER
        ) || !hasPermission || prefs.isLowPowerModeOn
    }

    private fun calculateDistance(): Distance {
        val paceCalculator = StrideLengthPaceCalculator(prefs.pedometer.strideLength)
        return paceCalculator.distance(_steps.value).meters()
    }


    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: PedometerSubsystem? = null

        @Synchronized
        fun getInstance(context: Context): PedometerSubsystem {
            if (instance == null) {
                instance = PedometerSubsystem(context.applicationContext)
            }
            return instance!!
        }
    }


}
