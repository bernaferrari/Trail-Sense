package com.kylecorry.trail_sense.tools.weather

/** Tool broadcasts and services of the weather tool; the values are stable ids other tools subscribe to. */
object WeatherBroadcasts {
    const val MONITOR_ENABLED = "weather-broadcast-weather-monitor-enabled"
    const val MONITOR_DISABLED = "weather-broadcast-weather-monitor-disabled"
    const val MONITOR_STATE_CHANGED = "weather-broadcast-weather-monitor-state-changed"
    const val MONITOR_FREQUENCY_CHANGED = "paths-broadcast-weather-monitor-frequency-changed"
    const val PREDICTION_CHANGED = "weather-broadcast-weather-prediction-changed"
    const val PARAM_MONITOR_FREQUENCY = "paths-broadcast-param-weather-monitor-frequency"

    /** The id of the weather monitor's tool service. */
    const val SERVICE_WEATHER_MONITOR = "weather-service-weather-monitor"
}
