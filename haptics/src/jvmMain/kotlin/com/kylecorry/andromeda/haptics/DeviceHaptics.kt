package com.kylecorry.andromeda.haptics

import kotlin.time.Duration

actual fun deviceHaptics(): IHapticMotor = UnavailableHaptics

private object UnavailableHaptics : IHapticMotor {
    override fun on(amplitude: Int) = Unit
    override fun feedback(feedbackType: HapticFeedbackType) = Unit
    override fun off() = Unit
    override fun once(onDuration: Duration, amplitude: Int) = Unit
    override fun interval(onDuration: Duration, offDuration: Duration, amplitude: Int) = Unit
    override fun pattern(durations: List<Duration>, amplitudes: List<Int>?, repeat: Boolean) = Unit
    override fun isAvailable() = false
}
