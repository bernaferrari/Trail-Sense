package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.battery.BatteryChargingStatus
import com.kylecorry.andromeda.battery.BatteryHealth
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class SensorAccuracy { PowerSave, Balanced, HighPrecision }

/** Altitude uses the platform location datum; mslAltitudeMeters is mean sea level where available. */
data class LocationReading(
    val coordinate: Coordinate,
    val altitudeMeters: Double?,
    val horizontalAccuracyMeters: Float? = null,
    val verticalAccuracyMeters: Float? = null,
    val speedMetersPerSecond: Float? = null,
    val bearingDegrees: Float? = null,
    val speedAccuracyMetersPerSecond: Float? = null,
    val bearingAccuracyDegrees: Float? = null,
    val mslAltitudeMeters: Double? = null
)
data class BatteryReading(
    val percent: Int?,
    val chargingStatus: BatteryChargingStatus,
    val health: BatteryHealth?,
    val temperatureCelsius: Float?
)

/** Cross-platform device observations. */
class DeviceSensors {
    internal val backend = createDeviceBackend()

    fun barometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowBarometer(pressure(interval, accuracy), scope)

    /** Atmospheric pressure in hectopascals. */
    fun pressure(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Float>> =
        backend.pressure(interval, accuracy)

    fun accelerometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowAccelerometer(acceleration(interval, accuracy), scope)

    /** Acceleration including gravity, in meters per second squared. */
    fun acceleration(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Vector3>> =
        backend.acceleration(interval, accuracy)

    /** Angular velocity in degrees per second. */
    fun angularVelocity(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Vector3>> =
        backend.angularVelocity(interval, accuracy)

    fun magnetometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowMagnetometer(magneticField(interval, accuracy), scope)

    /** Magnetic field in microteslas. */
    fun magneticField(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Vector3>> =
        backend.magneticField(interval, accuracy)

    fun battery(): Flow<Reading<BatteryReading>> = backend.battery()

    fun location(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<LocationReading>> =
        backend.location(interval, accuracy)
}

internal interface DeviceBackend {
    fun battery(): Flow<Reading<BatteryReading>>
    fun angularVelocity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>>
    fun magneticField(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>>
    fun pressure(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>>
    fun location(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<LocationReading>>
    fun acceleration(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>>
}

internal expect fun createDeviceBackend(): DeviceBackend
