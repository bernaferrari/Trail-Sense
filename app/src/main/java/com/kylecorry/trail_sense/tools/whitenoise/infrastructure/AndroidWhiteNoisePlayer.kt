package com.kylecorry.trail_sense.tools.whitenoise.infrastructure

import android.content.Context
import kotlin.time.Duration

class AndroidWhiteNoisePlayer(private val context: Context) : WhiteNoisePlayer {
    override val isRunning: Boolean
        get() = WhiteNoiseService.isRunning

    override fun play(sleepSound: SleepSound?, duration: Duration?) {
        WhiteNoiseService.play(context, sleepSound, duration)
    }

    override fun stop() {
        WhiteNoiseService.stop(context)
    }

    override fun clearSleepTimer() {
        WhiteNoiseService.clearSleepTimer(context)
    }
}
