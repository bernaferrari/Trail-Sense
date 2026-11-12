package com.kylecorry.trail_sense.tools.waterpurification.infrastructure

import android.content.Context

class AndroidWaterPurificationTimer(private val context: Context) : WaterPurificationTimer {
    override fun start(seconds: Long) {
        WaterPurificationTimerService.start(context, seconds)
    }

    override fun stop() {
        WaterPurificationTimerService.stop(context)
    }
}
