package com.kylecorry.trail_sense.tools.weather.infrastructure

/** Restarts a running weather monitor so it picks up a changed frequency; supplied by the platform. */
fun interface WeatherMonitorRestarter {
    fun restart()
}
