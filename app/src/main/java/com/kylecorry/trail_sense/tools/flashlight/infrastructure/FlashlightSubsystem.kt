package com.kylecorry.trail_sense.tools.flashlight.infrastructure

import com.kylecorry.trail_sense.settings.PreferenceKeys
import kotlin.time.Duration.Companion.seconds
import com.kylecorry.andromeda.torch.isTorchAvailable

import kotlin.time.Clock


import android.annotation.SuppressLint
import android.content.Context
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.andromeda.core.tryOrDefault
import com.kylecorry.andromeda.core.tryOrLog
import com.kylecorry.andromeda.torch.Torch
import com.kylecorry.andromeda.torch.deviceTorch
import com.kylecorry.andromeda.torch.TorchState
import com.kylecorry.andromeda.torch.torchStates
import com.kylecorry.sol.math.interpolation.Interpolation
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.tools.flashlight.FlashlightToolRegistration
import com.kylecorry.trail_sense.tools.flashlight.domain.FlashlightMode
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Optional
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FlashlightSubsystem private constructor(private val context: Context) : IFlashlightSubsystem, FlashlightSwitch {

    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }
    private val prefs by lazy { UserPreferences(context) }
    private var torch: Torch? = null

    private val transitionDuration = 1.seconds

    private val _mode = MutableStateFlow(FlashlightMode.Off)
    override val mode: StateFlow<FlashlightMode> = _mode.asStateFlow()

    override var selectedMode: FlashlightMode = FlashlightMode.Torch

    override val brightnessLevels: Int
        get() = (torch?.brightnessLevels ?: 1) - 1

    private var isAvailable: Boolean = isTorchAvailable(context, true)

    private var isTransitioning = false
    private val transitionTimer = CoroutineTimer {
        isTransitioning = false
    }

    private var brightness: Float = 1f

    private val modeLock = Any()
    private val torchLock = Any()
    private val systemMonitorLock = Any()

    private val scope = CoroutineScope(Dispatchers.Default)

    private var systemMonitorCount = 0
    private var systemMonitorJob: Job? = null

    init {
        scope.launch {
            _mode.collect {
                Tools.broadcast(FlashlightToolRegistration.BROADCAST_FLASHLIGHT_STATE_CHANGED)
            }
        }
        scope.launch {
            brightness = prefs.flashlight.brightness
            torch = deviceTorch()
            isAvailable = isTorchAvailable(context)
        }
    }

    fun startSystemMonitor() {
        synchronized(systemMonitorLock) {
            systemMonitorCount++
            if (systemMonitorCount == 1) {
                systemMonitorJob = scope.launch {
                    torchStates().collect { state ->
                        when (state) {
                            TorchState.On -> onTorchStateChanged(true)
                            TorchState.Off -> onTorchStateChanged(false)
                            TorchState.Unavailable -> Unit
                        }
                    }
                }
            }
        }
    }

    fun stopSystemMonitor() {
        synchronized(systemMonitorLock) {
            systemMonitorCount--
            if (systemMonitorCount == 0) {
                systemMonitorJob?.cancel()
                systemMonitorJob = null
            }
        }
    }

    private fun setTransitioning(transitioning: Boolean) {
        isTransitioning = transitioning
        if (transitioning) {
            transitionTimer.once(transitionDuration)
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
        val previousMode = getMode()
        _mode.value = newMode
        if (previousMode == FlashlightMode.Off) {
            FlashlightService.start(context)
        }
    }

    private fun off(bySystem: Boolean = false) = synchronized(modeLock) {
        clearTimeout()
        setTransitioning(!bySystem)
        _mode.value = FlashlightMode.Off
        FlashlightService.stop(context)
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

    override fun getMode(): FlashlightMode {
        return mode.value
    }

    override fun isAvailable(): Boolean {
        return isAvailable
    }

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
            }

            if (enabled && getMode() == FlashlightMode.Off) {
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
            cache.putInstant(
                PreferenceKeys.flashlightTimeoutInstant,
                kotlin.time.Clock.System.now().plus(prefs.flashlight.timeout)
            )
        } else {
            clearTimeout()
        }
    }

    private fun clearTimeout() {
        cache.remove(PreferenceKeys.flashlightTimeoutInstant)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: FlashlightSubsystem? = null
        fun getInstance(context: Context): FlashlightSubsystem {
            return instance ?: FlashlightSubsystem(context.applicationContext).also {
                instance = it
            }
        }
    }

}
