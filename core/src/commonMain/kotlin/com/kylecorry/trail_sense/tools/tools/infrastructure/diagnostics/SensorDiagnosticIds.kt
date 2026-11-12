package com.kylecorry.trail_sense.tools.tools.infrastructure.diagnostics

/** The ids the location and compass diagnostic scanners report, shared with the screens that react to them. */
object SensorDiagnosticIds {
    const val LOCATION_UNSET = "location-unset"
    const val LOCATION_OVERRIDDEN = "location-overridden"
    const val GPS_UNAVAILABLE = "gps-unavailable"
    const val GPS_POOR = "gps-poor"
    const val GPS_TIMED_OUT = "gps-timed-out"
    const val MAGNETOMETER_UNAVAILABLE = "magnetometer-unavailable"
    const val MAGNETOMETER_POOR = "magnetometer-poor"
}
