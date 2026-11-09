package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.sensors.SharedSensor
import com.kylecorry.andromeda.sense.location.GpsSensor
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Reading
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FlowGPS(private val readings: Flow<Reading<LocationReading>>, private val scope: CoroutineScope) : SharedSensor(), GpsSensor {
    override var hasValidReading = false
        private set
    override var quality = Quality.Unknown
        private set
    override var location = Coordinate.zero
        private set
    override var altitude = 0f
        private set
    override var mslAltitude: Float? = null
        private set
    override var horizontalAccuracy: Float? = null
        private set
    override var verticalAccuracy: Float? = null
        private set
    override var speed = Speed.from(0f, DistanceUnits.Meters, TimeUnits.Seconds)
        private set
    override var speedAccuracy: Float? = null
        private set
    override var rawBearing: Float? = null
        private set
    override val bearing: Bearing? get() = rawBearing?.let(Bearing::from)
    override var bearingAccuracy: Float? = null
        private set
    private var collection: Job? = null

    override fun startImpl() {
        collection = scope.launch(start = CoroutineStart.LAZY) {
            readings.collect { reading ->
                val value = reading.value
                location = value.coordinate
                altitude = value.altitudeMeters?.toFloat() ?: 0f
                mslAltitude = value.mslAltitudeMeters?.toFloat()
                horizontalAccuracy = value.horizontalAccuracyMeters
                verticalAccuracy = value.verticalAccuracyMeters
                speed = Speed.from(value.speedMetersPerSecond ?: 0f, DistanceUnits.Meters, TimeUnits.Seconds)
                speedAccuracy = value.speedAccuracyMetersPerSecond
                rawBearing = value.bearingDegrees
                bearingAccuracy = value.bearingAccuracyDegrees
                quality = when {
                    horizontalAccuracy == null -> Quality.Unknown
                    horizontalAccuracy!! < 8f -> Quality.Good
                    horizontalAccuracy!! < 16f -> Quality.Moderate
                    else -> Quality.Poor
                }
                hasValidReading = true
                setEventTime(reading.time)
                notifyListeners()
            }
        }
        collection?.start()
    }

    override fun stopImpl() {
        collection?.cancel()
        collection = null
    }
}
