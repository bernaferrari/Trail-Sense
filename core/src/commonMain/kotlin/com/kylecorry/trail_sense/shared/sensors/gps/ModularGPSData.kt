package com.kylecorry.trail_sense.shared.sensors.gps

import kotlin.concurrent.Volatile

import kotlin.time.Clock

import com.kylecorry.andromeda.core.sensors.SensorSubscriber
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.sense.location.GpsSensor
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import kotlin.time.Instant

class ModularGPSData(
    override var location: Coordinate = Coordinate.zero,
    override var verticalAccuracy: Float? = null,
    override var horizontalAccuracy: Float? = null,
    override var bearing: Bearing? = null,
    override var rawBearing: Float? = null,
    override var bearingAccuracy: Float? = null,
    override var speedAccuracy: Float? = null,
    override var eventTimeElapsedNanos: Long = 0L,
    override var quality: Quality = Quality.Unknown,
    override var hasValidReading: Boolean = false,
    override var altitude: Float = 0f,
    var eventTime: Instant = Clock.System.now(),
    override var speed: Speed = Speed.from(0f, DistanceUnits.Meters, TimeUnits.Seconds),
    @Volatile var isTimedOut: Boolean = false
) : GpsSensor {

    // Internal filter uncertainty - replace this with a custom data property (map) later
    var kalmanState: GPSKalmanState? = null
    var speedSource: SpeedSource = SpeedSource.Unknown

    override val mslAltitude: Float? = null

    val id: Long
        get() = eventTimeElapsedNanos

    override val timestamp: kotlin.time.Instant
        get() = eventTime

    // This is a data holder, so it never emits
    override fun start(subscriber: SensorSubscriber) {
        // Do nothing
    }

    override fun stop(subscriber: SensorSubscriber?) {
        // Do nothing
    }

    override fun subscribe(subscriber: SensorSubscriber) {
        // Do nothing
    }

    override fun unsubscribe(subscriber: SensorSubscriber) {
        // Do nothing
    }

    override fun unsubscribeAll() {
        // Do nothing
    }

    fun copyInto(other: ModularGPSData) {
        other.kalmanState = kalmanState
        other.location = location
        other.verticalAccuracy = verticalAccuracy
        other.horizontalAccuracy = horizontalAccuracy
        other.bearing = bearing
        other.rawBearing = rawBearing
        other.bearingAccuracy = bearingAccuracy
        other.speedAccuracy = speedAccuracy
        other.eventTimeElapsedNanos = eventTimeElapsedNanos
        other.quality = quality
        other.hasValidReading = hasValidReading
        other.altitude = altitude
        other.eventTime = eventTime
        other.speed = speed
        other.speedSource = speedSource
        other.isTimedOut = isTimedOut
    }

    fun populateFromGPS(gps: GpsSensor) {
        kalmanState = null
        location = gps.location
        verticalAccuracy = gps.verticalAccuracy
        horizontalAccuracy = gps.horizontalAccuracy
        bearing = gps.bearing
        rawBearing = gps.rawBearing
        bearingAccuracy = gps.bearingAccuracy
        speedAccuracy = gps.speedAccuracy
        eventTimeElapsedNanos = gps.eventTimeElapsedNanos
        quality = gps.quality
        hasValidReading = gps.hasValidReading
        altitude = gps.altitude
        eventTime = gps.timestamp
        speed = gps.speed
        speedSource = (gps as? ModularGPSData)?.speedSource ?: SpeedSource.Provider
    }
}
