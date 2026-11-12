package com.kylecorry.trail_sense.tools.waterpurification.infrastructure

/** Runs the water boil countdown outside the screen (a foreground service on Android). */
interface WaterPurificationTimer {
    fun start(seconds: Long)
    fun stop()
}
