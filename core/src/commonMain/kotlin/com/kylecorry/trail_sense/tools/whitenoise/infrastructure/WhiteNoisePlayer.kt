package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import kotlin.time.Duration

/** Plays the sleep sounds outside the screen (a foreground service on Android). */
interface WhiteNoisePlayer {
    val isRunning: Boolean

    /** Plays [sleepSound] (or the last one picked), stopping after [duration] when it is set. */
    fun play(sleepSound: SleepSound? = null, duration: Duration? = null)
    fun stop()
    fun clearSleepTimer()

    companion object {
        const val CACHE_KEY_OFF_TIME = "cache_white_noise_off_at"
        const val CACHE_KEY_SLEEP_SOUND_ID = "cache_sleep_sound_id"
    }
}
