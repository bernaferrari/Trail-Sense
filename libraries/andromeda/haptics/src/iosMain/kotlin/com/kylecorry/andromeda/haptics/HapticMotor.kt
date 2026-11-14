package com.kylecorry.andromeda.haptics

import com.swmansion.pulsar.kmp.Pulsar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/** iOS haptic motor backed by Core Haptics. */
internal class IosHapticMotor : HapticMotor {
    private val pulsar = Pulsar.create()
    private val realtime = pulsar.getRealtimeComposer()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var playback: Job? = null

    override fun isAvailable() = pulsar.isHapticsSupported()

    override fun on(amplitude: Int) = once(1.days, amplitude)

    override fun off() {
        playback?.cancel()
        playback = null
        realtime.stop()
        pulsar.stopHaptics()
    }

    override fun feedback(feedbackType: HapticFeedbackType) {
        off()
        val presets = pulsar.getPresets()
        when (feedbackType) {
            HapticFeedbackType.Tick -> presets.systemEffectTick()
            HapticFeedbackType.Click -> presets.systemEffectClick()
            HapticFeedbackType.HeavyClick -> presets.systemEffectHeavyClick()
            HapticFeedbackType.DoubleClick -> presets.systemEffectDoubleClick()
        }
    }

    override fun once(onDuration: Duration, amplitude: Int) =
        pattern(listOf(onDuration), listOf(amplitude), false)

    override fun interval(onDuration: Duration, offDuration: Duration, amplitude: Int) =
        pattern(listOf(onDuration, offDuration), listOf(amplitude, 0), true)

    override fun pattern(durations: List<Duration>, amplitudes: List<Int>?, repeat: Boolean) {
        require(amplitudes == null || amplitudes.size == durations.size)
        require(durations.all { it >= Duration.ZERO && it.isFinite() })
        off()
        if (durations.isEmpty() || durations.all { it == Duration.ZERO }) return
        playback = scope.launch {
            try {
                do {
                    durations.forEachIndexed { index, duration ->
                        val amplitude = amplitudes?.get(index) ?: if (index % 2 == 0) 0 else -1
                        if (amplitude == 0) realtime.stop()
                        else realtime.set(if (amplitude < 0) 1f else (amplitude / 255f).coerceIn(0f, 1f), 0.5f)
                        delay(duration)
                    }
                } while (repeat)
            } finally {
                if (playback === currentCoroutineContext()[Job]) realtime.stop()
            }
        }
    }
}
