package com.kylecorry.andromeda.haptics

import kotlin.time.Duration

public interface HapticMotor {
    /**
     * Turns the vibrator on (note: maximum 1 day)
     * @param amplitude the amplitude between 0 and 255, or -1 for the default amplitude
     */
    public fun on(amplitude: Int = -1)

    /**
     * Performs the specified haptic feedback
     */
    public fun feedback(feedbackType: HapticFeedbackType)

    /**
     * Turns the vibrator off
     */
    public fun off()

    /**
     * Turns the vibrator on for a duration
     * @param onDuration the duration to turn the vibrator on for
     * @param amplitude the amplitude between 0 and 255, or -1 for the default amplitude
     */
    public fun once(onDuration: Duration, amplitude: Int = -1)

    /**
     * Turns the vibrator on and off indefinitely (starts on)
     * @param onDuration the duration to turn the vibrator on for
     * @param offDuration the duration to turn the vibrator off for
     * @param amplitude the amplitude between 0 and 255, or -1 for the default amplitude
     */
    public fun interval(
        onDuration: Duration,
        offDuration: Duration,
        amplitude: Int = -1
    )

    /**
     * Turns the vibrator on and off based on a pattern (starts off by default, override with amplitudes)
     * @param durations the duration to turn the vibrator on or off for
     * @param amplitudes the amplitudes between 0 and 255, or -1 for the default amplitude. Must correspond with the durations array.
     * @param repeat true to repeat indefinitely, defaults to false
     */
    public fun pattern(
        durations: List<Duration>,
        amplitudes: List<Int>? = null,
        repeat: Boolean = false
    )

    /**
     * Determines if the device has a vibrator
     * @return true if a vibrator exists, false otherwise
     */
    public fun isAvailable(): Boolean
}