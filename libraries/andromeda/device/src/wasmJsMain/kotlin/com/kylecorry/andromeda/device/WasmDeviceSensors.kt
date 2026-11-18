package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlin.time.Duration

internal actual fun createDeviceBackend(): DeviceBackend = object : DeviceBackend {
    override fun attitude(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<DeviceAttitude>> = emptyFlow()
    override fun gravity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
    override fun linearAcceleration(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
    override fun magneticHeading(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun illuminance(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun ambientTemperature(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun relativeHumidity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun steps(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Int>> = emptyFlow()
    override fun proximity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Boolean>> = emptyFlow()
    override fun battery(): Flow<Reading<BatteryReading>> = emptyFlow()
    override fun angularVelocity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
    override fun magneticField(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
    override fun pressure(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>> = emptyFlow()
    override fun location(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<LocationReading>> = callbackFlow {
        val id = watchLocation(
            accuracy == SensorAccuracy.HighPrecision,
            interval.inWholeMilliseconds.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
            onLocation = { latitude, longitude, altitude, horizontal, vertical, speed, heading, milliseconds ->
                val reading = LocationReading(
                    coordinate = Coordinate(latitude, longitude),
                    altitudeMeters = altitude.takeIf { it.isFinite() },
                    horizontalAccuracyMeters = horizontal.toFloat().takeIf { it.isFinite() },
                    verticalAccuracyMeters = vertical.toFloat().takeIf { it.isFinite() },
                    speedMetersPerSecond = speed.toFloat().takeIf { it.isFinite() },
                    bearingDegrees = heading.toFloat().takeIf { it.isFinite() }
                )
                trySend(Reading(reading, Instant.fromEpochMilliseconds(milliseconds.toLong())))
            },
            onDenied = { close() }
        )
        if (id < 0) close()
        awaitClose { if (id >= 0) clearLocationWatch(id) }
    }
    override fun acceleration(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>> = emptyFlow()
}

internal actual fun platformSatelliteStatus(): Flow<Reading<List<Satellite>>> = emptyFlow()


@OptIn(ExperimentalWasmJsInterop::class)
private fun watchLocation(
    highAccuracy: Boolean,
    maximumAge: Int,
    onLocation: (Double, Double, Double, Double, Double, Double, Double, Double) -> Unit,
    onDenied: () -> Unit
): Int = js(
    """{
        if (!navigator.geolocation) return -1;
        return navigator.geolocation.watchPosition(({coords, timestamp}) => onLocation(
            coords.latitude, coords.longitude, coords.altitude ?? NaN, coords.accuracy ?? NaN,
            coords.altitudeAccuracy ?? NaN, coords.speed ?? NaN, coords.heading ?? NaN, timestamp
        ), error => {
            if (error.code === error.PERMISSION_DENIED) onDenied();
        }, {enableHighAccuracy: highAccuracy, maximumAge});
    }"""
)

@OptIn(ExperimentalWasmJsInterop::class)
private fun clearLocationWatch(id: Int): Unit = js("navigator.geolocation.clearWatch(id)")
