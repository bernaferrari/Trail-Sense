package com.kylecorry.trail_sense.tools.turn_back.ui

/** The turn back alarm and the permissions it needs, supplied by the platform shell. */
interface TurnBackAlarm {
    /** Schedules the turn back and return alerts from the stored times, asking for any permissions they need. */
    suspend fun start()

    /** Cancels the scheduled alerts. */
    fun stop()
}
