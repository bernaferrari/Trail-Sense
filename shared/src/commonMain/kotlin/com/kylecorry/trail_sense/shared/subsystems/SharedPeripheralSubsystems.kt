package com.kylecorry.trail_sense.shared.subsystems

import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.haptics.HapticFeedbackType
import com.kylecorry.andromeda.haptics.deviceHaptics
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.andromeda.sound.SoundPlayer
import com.kylecorry.andromeda.sound.streamPlayer
import com.kylecorry.andromeda.sound.tonePlayer
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.andromeda.torch.TorchState
import com.kylecorry.andromeda.torch.deviceTorch
import com.kylecorry.andromeda.torch.torchStates
import com.kylecorry.luna.concurrency.BackgroundTask
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.shared.FeatureState
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.events.EventData
import com.kylecorry.trail_sense.shared.events.ToolBroadcasts
import com.kylecorry.trail_sense.shared.haptics.IHapticSubsystem
import com.kylecorry.trail_sense.shared.preferences.onEachChange
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sound.TonePlayerFactory
import com.kylecorry.trail_sense.shared.withId
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.FlashlightSwitch
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.IFlashlightStrategy
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.IFlashlightSubsystem
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.SosFlashlightStrategy
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.StrobeFlashlightStrategy
import com.kylecorry.trail_sense.tools.flashlight.infrastructure.TorchFlashlightStrategy
import com.kylecorry.trail_sense.tools.pedometer.PedometerBroadcasts
import com.kylecorry.trail_sense.tools.pedometer.domain.IStepTrackerService
import com.kylecorry.trail_sense.tools.pedometer.domain.StrideLengthPaceCalculator
import com.kylecorry.trail_sense.tools.pedometer.infrastructure.subsystem.IPedometerSubsystem
import com.kylecorry.trail_sense.tools.waterpurification.infrastructure.WaterPurificationTimer
import com.kylecorry.trail_sense.tools.waterpurification.ui.WATER_PURIFICATION_END_TIME_KEY
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.SleepSound
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.WhiteNoisePlayer
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.BrownNoiseAudioStream
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.CricketsAudioStream
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.FanNoiseAudioStream
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.OceanWavesAudioStream
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.PinkNoiseAudioStream
import com.kylecorry.trail_sense.tools.whitenoise.infrastructure.streams.WhiteNoiseAudioStream
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Shared [IHapticSubsystem] backed by [deviceHaptics]. */
class SharedHapticSubsystem : IHapticSubsystem {
    private val motor = deviceHaptics()

    override fun off() {
        motor.off()
    }

    override fun tick() {
        motor.feedback(HapticFeedbackType.Tick)
    }

    override fun interval(on: Duration, off: Duration) {
        motor.interval(on, off)
    }

    override fun click() {
        motor.feedback(HapticFeedbackType.Click)
    }

    override fun alert() {
        motor.once(1.seconds)
    }
}

/** Multiplatform [IFlashlightSubsystem] backed by Andromeda [deviceTorch] and flashlight strategies. */
class SharedFlashlightSubsystem(
    private val cache: Preferences,
    private val prefs: UserPreferences
) : IFlashlightSubsystem, FlashlightSwitch {

    private var torch: Torch? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val modeLock = SynchronizedObject()
    private val torchLock = SynchronizedObject()

    private val _mode = MutableStateFlow(FlashlightMode.Off)
    override val mode: StateFlow<FlashlightMode> = _mode.asStateFlow()

    override var selectedMode: FlashlightMode = FlashlightMode.Torch

    override val brightnessLevels: Int
        get() = ((torch?.brightnessLevels ?: 1) - 1).coerceAtLeast(0)

    private var isAvailable: Boolean = false
    private var isTransitioning = false
    private val transitionTimer = CoroutineTimer(scope = scope) {
        isTransitioning = false
    }
    private val timeoutTimer = CoroutineTimer(scope = scope) {
        set(FlashlightMode.Off)
    }

    private var brightness: Float = 1f
    private var strategy: IFlashlightStrategy? = null

    init {
        torch = runCatching { deviceTorch() }.getOrNull()
        isAvailable = torch?.isAvailable() == true
        brightness = prefs.flashlight.brightness
        scope.launch {
            _mode.collect { newMode ->
                updateStrategy(newMode)
                ToolBroadcasts.broadcast(BROADCAST_FLASHLIGHT_STATE_CHANGED)
            }
        }
        scope.launch {
            torchStates().collect { state ->
                when (state) {
                    TorchState.On -> onTorchStateChanged(true)
                    TorchState.Off -> onTorchStateChanged(false)
                    TorchState.Unavailable -> Unit
                }
            }
        }
    }

    private fun updateStrategy(newMode: FlashlightMode) {
        val next = when (newMode) {
            FlashlightMode.Torch -> TorchFlashlightStrategy(this)
            FlashlightMode.Sos -> SosFlashlightStrategy(this)
            FlashlightMode.Off -> null
            else -> StrobeFlashlightStrategy(this, getInterval(newMode))
        }
        strategy?.stop()
        strategy = next
        strategy?.start()
    }

    private fun getInterval(mode: FlashlightMode): Duration {
        val frequency = when (mode) {
            FlashlightMode.Off, FlashlightMode.Torch, FlashlightMode.Sos -> 1
            FlashlightMode.Strobe1 -> 1
            FlashlightMode.Strobe2 -> 2
            FlashlightMode.Strobe3 -> 3
            FlashlightMode.Strobe4 -> 4
            FlashlightMode.Strobe5 -> 5
            FlashlightMode.Strobe6 -> 6
            FlashlightMode.Strobe7 -> 7
            FlashlightMode.Strobe8 -> 8
            FlashlightMode.Strobe9 -> 9
            FlashlightMode.Strobe200 -> 200
        }
        return (1000L / frequency).milliseconds
    }

    private fun setTransitioning(transitioning: Boolean) {
        isTransitioning = transitioning
        if (transitioning) {
            transitionTimer.once(1.seconds)
        } else {
            transitionTimer.stop()
        }
    }

    private fun on(newMode: FlashlightMode, bySystem: Boolean = false) = synchronized(modeLock) {
        clearTimeout()
        selectedMode = newMode
        setTransitioning(!bySystem)
        if (!bySystem) {
            setTimeout()
        }
        _mode.value = newMode
    }

    private fun off(bySystem: Boolean = false) = synchronized(modeLock) {
        clearTimeout()
        setTransitioning(!bySystem)
        _mode.value = FlashlightMode.Off
        torch?.off()
    }

    override fun toggle() {
        if (getMode() == FlashlightMode.Torch) {
            off()
        } else {
            set(FlashlightMode.Torch)
        }
    }

    override fun set(mode: FlashlightMode) {
        when (mode) {
            FlashlightMode.Off -> off()
            else -> on(mode)
        }
    }

    override fun getMode(): FlashlightMode = mode.value

    override fun isAvailable(): Boolean = isAvailable

    override fun turnOn() {
        synchronized(torchLock) {
            if (brightnessLevels > 0) {
                val mapped = Interpolation.map(brightness, 0f, 1f, 1f / (brightnessLevels + 1), 1f)
                torch?.on(mapped)
            } else {
                torch?.on()
            }
        }
    }

    override fun turnOff() {
        synchronized(torchLock) {
            torch?.off()
        }
    }

    private fun onTorchStateChanged(enabled: Boolean) {
        tryOrLog {
            synchronized(modeLock) {
                if (isTransitioning) {
                    return@tryOrLog
                }
            }
            if (!enabled && getMode() == FlashlightMode.Torch) {
                off(true)
            } else if (enabled && getMode() == FlashlightMode.Off) {
                setBrightness(1f)
                on(FlashlightMode.Torch, true)
            }
        }
    }

    override fun setBrightness(brightness: Float) {
        prefs.flashlight.brightness = brightness
        this.brightness = brightness
        if (getMode() == FlashlightMode.Torch) {
            turnOn()
        }
    }

    private fun setTimeout() {
        if (prefs.flashlight.shouldTimeout) {
            val timeout = prefs.flashlight.timeout
            cache.putInstant(PreferenceKeys.flashlightTimeoutInstant, Clock.System.now().plus(timeout))
            timeoutTimer.once(timeout)
        } else {
            clearTimeout()
        }
    }

    private fun clearTimeout() {
        timeoutTimer.stop()
        cache.remove(PreferenceKeys.flashlightTimeoutInstant)
    }

    companion object {
        const val BROADCAST_FLASHLIGHT_STATE_CHANGED = "flashlight-broadcast-flashlight-state-changed"
    }
}

/** Multiplatform [IPedometerSubsystem] backed by hardware pedometer sensors and [IStepTrackerService]. */
class SharedPedometerSubsystem(
    private val sharedPrefs: Preferences,
    private val prefs: UserPreferences,
    private val stepTrackerService: IStepTrackerService,
    private val sensorService: SensorService,
    private val hasStepCounterCheck: () -> Boolean = { true },
    private val hasActivityPermissionCheck: () -> Boolean = { true }
) : IPedometerSubsystem {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _steps = MutableStateFlow(0L)
    private val _distance = MutableStateFlow(calculateDistance())
    private val _state = MutableStateFlow(calculateState())

    override val steps: StateFlow<Long> = _steps.asStateFlow()
    override val distance: StateFlow<Distance> = _distance.asStateFlow()
    override val state: StateFlow<FeatureState> = _state.asStateFlow()

    private val pedometer = sensorService.getPedometer()
    private var lastSensorSteps: Int? = null
    private var lastStepTime: Instant? = null
    private var listening = false

    private val pedometerListener: () -> Boolean = {
        val current = pedometer.steps
        val now = Clock.System.now()
        val previous = lastSensorSteps
        val previousTime = lastStepTime
        lastSensorSteps = current
        lastStepTime = now
        val delta = if (previous != null && current >= previous) (current - previous).toLong() else 0L
        if (delta > 0 && previousTime != null) {
            val activeDuration = (now - previousTime).coerceIn(Duration.ZERO, 30.seconds)
            scope.launch {
                stepTrackerService.addSteps(delta, now, activeDuration)
            }
        }
        true
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

    init {
        BackgroundTask { updateSteps() }.start()
        ToolBroadcasts.subscribe(PedometerBroadcasts.STEPS_CHANGED) {
            BackgroundTask {
                stepsQueue.enqueue {
                    updateSteps(it)
                }
            }.start()
        }
        scope.launch {
            distance.drop(1).collect {
                ToolBroadcasts.broadcast(PedometerBroadcasts.DISTANCE_CHANGED)
            }
        }
        sharedPrefs.onEachChange { key ->
            if (key in stateChangePrefKeys) {
                _state.value = calculateState()
                syncSensorListener()
            }
            if (key in distanceChangePrefKeys) {
                _distance.value = calculateDistance()
            }
        }
        syncSensorListener()
    }

    override fun enable() {
        prefs.pedometer.isEnabled = true
        ToolBroadcasts.broadcast(PedometerBroadcasts.PEDOMETER_ENABLED)
        syncSensorListener()
    }

    override fun disable() {
        prefs.pedometer.isEnabled = false
        ToolBroadcasts.broadcast(PedometerBroadcasts.PEDOMETER_DISABLED)
        syncSensorListener()
    }

    override fun isDisabledDueToPermissions(): Boolean {
        return !hasActivityPermissionCheck() && !prefs.isLowPowerModeOn
    }

    private fun syncSensorListener() {
        val shouldListen = _state.value == FeatureState.On
        if (shouldListen && !listening) {
            listening = true
            lastSensorSteps = null
            lastStepTime = Clock.System.now()
            pedometer.start(pedometerListener)
        } else if (!shouldListen && listening) {
            listening = false
            pedometer.stop(pedometerListener)
            lastSensorSteps = null
            lastStepTime = null
        }
    }

    private suspend fun updateSteps(data: EventData? = null) = stepsMutex.withLock {
        val stepCount = data?.getLong(PedometerBroadcasts.PARAM_STEPS)
            ?: stepTrackerService.getOpenStepTrackingPeriod()?.steps
            ?: 0L
        _steps.value = stepCount
        _distance.value = calculateDistance()
    }

    private fun calculateState(): FeatureState {
        return if (!hasStepCounterCheck() || !hasActivityPermissionCheck() || prefs.isLowPowerModeOn) {
            FeatureState.Unavailable
        } else if (prefs.pedometer.isEnabled) {
            FeatureState.On
        } else {
            FeatureState.Off
        }
    }

    private fun calculateDistance(): Distance {
        val paceCalculator = StrideLengthPaceCalculator(prefs.pedometer.strideLength)
        return paceCalculator.distance(_steps.value).meters()
    }
}

/** Shared [TonePlayerFactory] backed by Andromeda's multiplatform [tonePlayer]. */
class SharedTonePlayerFactory : TonePlayerFactory {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun create(frequencyHz: Int): SoundPlayer {
        return tonePlayer(scope, frequencyHz.toDouble())
    }
}

/** Multiplatform [WhiteNoisePlayer] backed by Andromeda's [streamPlayer] and procedural audio streams. */
class SharedWhiteNoisePlayer(private val cache: Preferences) : WhiteNoisePlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var player: SoundPlayer? = null
    private val offTimer = CoroutineTimer(scope = scope) {
        stop()
    }

    override var isRunning: Boolean = false
        private set

    override fun play(sleepSound: SleepSound?, duration: Duration?) {
        val chosen = sleepSound ?: run {
            val savedId = cache.getLong(WhiteNoisePlayer.CACHE_KEY_SLEEP_SOUND_ID) ?: SleepSound.PinkNoise.id
            SleepSound.entries.withId(savedId) ?: SleepSound.PinkNoise
        }
        cache.putLong(WhiteNoisePlayer.CACHE_KEY_SLEEP_SOUND_ID, chosen.id)

        if (duration != null) {
            val endTime = Clock.System.now() + duration
            cache.putInstant(WhiteNoisePlayer.CACHE_KEY_OFF_TIME, endTime)
            offTimer.once(duration)
        } else {
            cache.remove(WhiteNoisePlayer.CACHE_KEY_OFF_TIME)
            offTimer.stop()
        }

        val stream = when (chosen) {
            SleepSound.WhiteNoise -> WhiteNoiseAudioStream()
            SleepSound.PinkNoise -> PinkNoiseAudioStream()
            SleepSound.BrownNoise -> BrownNoiseAudioStream()
            SleepSound.Crickets -> CricketsAudioStream()
            SleepSound.CricketsNoChirp -> CricketsAudioStream(false)
            SleepSound.OceanWaves -> OceanWavesAudioStream()
            SleepSound.Fan -> FanNoiseAudioStream()
        }

        player?.fadeOff(true)
        player = streamPlayer(scope, stream).also { it.fadeOn() }
        isRunning = true
        ToolBroadcasts.broadcast(BROADCAST_PLAYBACK_STATE_CHANGED)
    }

    override fun stop() {
        offTimer.stop()
        cache.remove(WhiteNoisePlayer.CACHE_KEY_OFF_TIME)
        player?.fadeOff(true)
        player = null
        val wasRunning = isRunning
        isRunning = false
        if (wasRunning) {
            ToolBroadcasts.broadcast(BROADCAST_PLAYBACK_STATE_CHANGED)
            ToolBroadcasts.broadcast(BROADCAST_PLAYBACK_FINISHED)
        }
    }

    override fun clearSleepTimer() {
        offTimer.stop()
        cache.remove(WhiteNoisePlayer.CACHE_KEY_OFF_TIME)
    }

    companion object {
        const val BROADCAST_PLAYBACK_STATE_CHANGED = "whitenoise-broadcast-playback-state-changed"
        const val BROADCAST_PLAYBACK_FINISHED = "whitenoise-broadcast-playback-finished"
    }
}

/** Multiplatform [WaterPurificationTimer] backed by [CoroutineTimer] and [Preferences]. */
class SharedWaterPurificationTimer(
    private val prefs: Preferences
) : WaterPurificationTimer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private val timer = CoroutineTimer(scope = scope) {
        prefs.remove(WATER_PURIFICATION_END_TIME_KEY)
    }

    override fun start(seconds: Long) {
        job?.cancel()
        val delayDuration = seconds.coerceAtLeast(0L).seconds
        timer.once(delayDuration)
    }

    override fun stop() {
        job?.cancel()
        timer.stop()
        prefs.remove(WATER_PURIFICATION_END_TIME_KEY)
    }
}
