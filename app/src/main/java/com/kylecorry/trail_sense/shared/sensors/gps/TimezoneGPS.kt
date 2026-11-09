package com.kylecorry.trail_sense.shared.sensors.gps

import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

import android.os.SystemClock
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.luna.hooks.Hooks
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.trail_sense.shared.sensors.IntervalSensor
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import com.kylecorry.sol.time.Time

class TimezoneGPS(updateFrequency: Long = 1000L) :
    IntervalSensor(updateFrequency.milliseconds),
    ISatelliteGPS, InactiveGPS, MockedGPS {

    private val hooks = Hooks()

    override val location: Coordinate
        get() {
            val zone = TimeZone.currentSystemDefault()
            return hooks.memo("location", zone) {
                Time.getLocationFromTimeZone(zone)
            }
        }
    override val speed: Speed
        get() = Speed.from(0f, DistanceUnits.Meters, TimeUnits.Seconds)
    override val speedAccuracy: Float?
        get() = null
    override var timestamp: kotlin.time.Instant
        get() = kotlin.time.Clock.System.now()
        set(_) {}
    override val verticalAccuracy: Float?
        get() = null
    override val horizontalAccuracy: Float?
        get() = null
    override val satellites: Int
        get() = 0
    override val hasValidReading: Boolean
        get() = true
    override val altitude: Float
        get() = 0f
    override val bearing: Bearing?
        get() = null
    override val bearingAccuracy: Float?
        get() = null
    override var eventTimeElapsedNanos: Long
        get() = SystemClock.elapsedRealtimeNanos()
        set(_) {}
    override val mslAltitude: Float
        get() = altitude
    override val rawBearing: Float?
        get() = null
    override val satelliteDetails: List<Satellite>?
        get() = null
}
