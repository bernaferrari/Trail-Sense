package com.kylecorry.trail_sense.tools.paths.infrastructure

/** Restarts Backtrack, when it is on, so it picks up a changed frequency; supplied by the platform. */
fun interface BacktrackRestarter {
    suspend fun restart()
}
