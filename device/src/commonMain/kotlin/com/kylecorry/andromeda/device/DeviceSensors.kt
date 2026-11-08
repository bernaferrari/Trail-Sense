package com.kylecorry.andromeda.device

import com.kylecorry.andromeda.battery.BatteryChargingStatus
import com.kylecorry.andromeda.battery.BatteryHealth
import com.kylecorry.andromeda.battery.BatteryChargingMethod
import com.kylecorry.andromeda.sense.location.Satellite
import com.kylecorry.sol.math.Vector3
import com.kylecorry.sol.math.Quaternion
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Reading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

enum class SensorAccuracy { PowerSave, Balanced, HighPrecision }

/** Device-to-world orientation in the magnetic east–north–up frame. */
data class DeviceAttitude(val orientation: Quaternion, val headingAccuracyDegrees: Float? = null)

internal fun northWestUpToEastNorthUp(value: Quaternion): Quaternion {
    val c = kotlin.math.sqrt(0.5f)
    return Quaternion(c * (value.x - value.y), c * (value.x + value.y), c * (value.z + value.w), c * (value.w - value.z))
}

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
    val temperatureCelsius: Float?,
    val voltageVolts: Float? = null,
    val currentMilliamps: Float? = null,
    val chargeMilliampHours: Float? = null,
    val fullChargeMilliampHours: Float? = null,
    val chargingMethod: BatteryChargingMethod? = null,
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

    fun compass(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowCompass(magneticHeading(interval, accuracy), scope)

    /** Magnetic heading in degrees clockwise from magnetic north. */
    fun magneticHeading(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Float>> =
        backend.magneticHeading(interval, accuracy)

    /** Fused device attitude in the magnetic east–north–up frame, where available. */
    fun attitude(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<DeviceAttitude>> =
        backend.attitude(interval, accuracy)

    fun orientationSensor(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowOrientationSensor(attitude(interval, accuracy), scope)

    fun gravitySensor(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowAccelerometer(gravity(interval, accuracy), scope)

    /** Gravity acceleration in meters per second squared. */
    fun gravity(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Vector3>> =
        backend.gravity(interval, accuracy)

    fun linearAccelerometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowAccelerometer(linearAcceleration(interval, accuracy), scope)

    /** Acceleration excluding gravity, in meters per second squared. */
    fun linearAcceleration(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Vector3>> =
        backend.linearAcceleration(interval, accuracy)

    /** Gyroscope with an integrated relative orientation. */
    fun gyroscope(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowGyroscope(angularVelocity(interval, accuracy), scope)

    /** Angular velocity in degrees per second. */
    fun angularVelocity(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Vector3>> =
        backend.angularVelocity(interval, accuracy)

    fun magnetometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowMagnetometer(magneticField(interval, accuracy), scope)

    /** Magnetic field in microteslas. */
    fun magneticField(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Vector3>> =
        backend.magneticField(interval, accuracy)

    fun battery(): Flow<Reading<BatteryReading>> = backend.battery()

    fun lightSensor(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowLightSensor(illuminance(interval, accuracy), scope)

    fun hygrometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowHygrometer(relativeHumidity(interval, accuracy), scope)

    fun thermometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowThermometer(ambientTemperature(interval, accuracy), scope)

    fun pedometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowPedometer(steps(interval, accuracy), scope)

    /** Ambient illuminance in lux, where supported. */
    fun illuminance(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Float>> =
        backend.illuminance(interval, accuracy)

    /** Ambient temperature in degrees Celsius, where supported. */
    fun ambientTemperature(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Float>> =
        backend.ambientTemperature(interval, accuracy)

    /** Relative humidity in percent, where supported. */
    fun relativeHumidity(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Float>> =
        backend.relativeHumidity(interval, accuracy)

    /** Cumulative step count reported by the platform. */
    fun steps(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Int>> =
        backend.steps(interval, accuracy)

    /** Whether an object is close to the device, where supported. */
    fun proximity(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Boolean>> =
        backend.proximity(interval, accuracy)

    /** Mean-sea-level altitude in meters, when the platform supplies it. */
    fun mslAltitude(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Float>> =
        location(interval, accuracy).mapNotNull { reading ->
            reading.value.mslAltitudeMeters?.let { Reading(it.toFloat(), reading.time) }
        }

    fun mslAltimeter(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowAltimeter(mslAltitude(interval, accuracy), scope)

    /** GNSS satellite details where the platform provides them. */
    fun satelliteStatus(): Flow<Reading<List<Satellite>>> = platformSatelliteStatus()

    fun satelliteStatusSensor(scope: CoroutineScope) = FlowSatelliteStatus(satelliteStatus(), scope)

    /** Monitor circular geofences while location is being collected. */
    fun geofenceTransitions(
        fences: List<Geofence>,
        interval: Duration = 1.seconds,
        accuracy: SensorAccuracy = SensorAccuracy.Balanced,
    ): Flow<GeofenceTransition> = location(interval, accuracy).geofenceTransitions(fences)

    fun gps(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced) =
        FlowGPS(location(interval, accuracy), scope)

    fun location(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<LocationReading>> =
        backend.location(interval, accuracy)
}

internal interface DeviceBackend {
    fun attitude(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<DeviceAttitude>>
    fun gravity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>>
    fun linearAcceleration(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>>
    fun magneticHeading(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>>
    fun illuminance(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>>
    fun ambientTemperature(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>>
    fun relativeHumidity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>>
    fun steps(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Int>>
    fun proximity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Boolean>>
    fun battery(): Flow<Reading<BatteryReading>>
    fun angularVelocity(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>>
    fun magneticField(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>>
    fun pressure(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Float>>
    fun location(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<LocationReading>>
    fun acceleration(interval: Duration, accuracy: SensorAccuracy): Flow<Reading<Vector3>>
}

internal expect fun createDeviceBackend(): DeviceBackend
internal expect fun platformSatelliteStatus(): Flow<Reading<List<Satellite>>>
