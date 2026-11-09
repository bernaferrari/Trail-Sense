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

public enum class SensorAccuracy { PowerSave, Balanced, HighPrecision }

/** Device-to-world orientation in the magnetic east–north–up frame. */
public data class DeviceAttitude(val orientation: Quaternion, val headingAccuracyDegrees: Float? = null)

internal fun northWestUpToEastNorthUp(value: Quaternion): Quaternion {
    val c = kotlin.math.sqrt(0.5f)
    return Quaternion(c * (value.x - value.y), c * (value.x + value.y), c * (value.z + value.w), c * (value.w - value.z))
}

/** Altitude uses the platform location datum; mslAltitudeMeters is mean sea level where available. */
public data class LocationReading(
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
public data class BatteryReading(
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
public class DeviceSensors {
    internal val backend = createDeviceBackend()

    public fun barometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): FlowBarometer =
        FlowBarometer(pressure(interval, accuracy), scope)

    /** Atmospheric pressure in hectopascals. */
    public fun pressure(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Float>> =
        backend.pressure(interval, accuracy)

    public fun accelerometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): FlowAccelerometer =
        FlowAccelerometer(acceleration(interval, accuracy), scope)

    /** Acceleration including gravity, in meters per second squared. */
    public fun acceleration(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Vector3>> =
        backend.acceleration(interval, accuracy)

    public fun compass(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): FlowCompass =
        FlowCompass(magneticHeading(interval, accuracy), scope)

    /** Magnetic heading in degrees clockwise from magnetic north. */
    public fun magneticHeading(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Float>> =
        backend.magneticHeading(interval, accuracy)

    /** Fused device attitude in the magnetic east–north–up frame, where available. */
    public fun attitude(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<DeviceAttitude>> =
        backend.attitude(interval, accuracy)

    public fun orientationSensor(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): FlowOrientationSensor =
        FlowOrientationSensor(attitude(interval, accuracy), scope)

    public fun gravitySensor(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): FlowAccelerometer =
        FlowAccelerometer(gravity(interval, accuracy), scope)

    /** Gravity acceleration in meters per second squared. */
    public fun gravity(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Vector3>> =
        backend.gravity(interval, accuracy)

    public fun linearAccelerometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): FlowAccelerometer =
        FlowAccelerometer(linearAcceleration(interval, accuracy), scope)

    /** Acceleration excluding gravity, in meters per second squared. */
    public fun linearAcceleration(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Vector3>> =
        backend.linearAcceleration(interval, accuracy)

    /** Gyroscope with an integrated relative orientation. */
    public fun gyroscope(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): FlowGyroscope =
        FlowGyroscope(angularVelocity(interval, accuracy), scope)

    /** Angular velocity in degrees per second. */
    public fun angularVelocity(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Vector3>> =
        backend.angularVelocity(interval, accuracy)

    public fun magnetometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): FlowMagnetometer =
        FlowMagnetometer(magneticField(interval, accuracy), scope)

    /** Magnetic field in microteslas. */
    public fun magneticField(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Vector3>> =
        backend.magneticField(interval, accuracy)

    public fun battery(): Flow<Reading<BatteryReading>> = backend.battery()

    public fun lightSensor(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): FlowLightSensor =
        FlowLightSensor(illuminance(interval, accuracy), scope)

    public fun hygrometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): FlowHygrometer =
        FlowHygrometer(relativeHumidity(interval, accuracy), scope)

    public fun thermometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): FlowThermometer =
        FlowThermometer(ambientTemperature(interval, accuracy), scope)

    public fun pedometer(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): FlowPedometer =
        FlowPedometer(steps(interval, accuracy), scope)

    /** Ambient illuminance in lux, where supported. */
    public fun illuminance(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Float>> =
        backend.illuminance(interval, accuracy)

    /** Ambient temperature in degrees Celsius, where supported. */
    public fun ambientTemperature(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Float>> =
        backend.ambientTemperature(interval, accuracy)

    /** Relative humidity in percent, where supported. */
    public fun relativeHumidity(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Float>> =
        backend.relativeHumidity(interval, accuracy)

    /** Cumulative step count reported by the platform. */
    public fun steps(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Int>> =
        backend.steps(interval, accuracy)

    /** Whether an object is close to the device, where supported. */
    public fun proximity(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Boolean>> =
        backend.proximity(interval, accuracy)

    /** Mean-sea-level altitude in meters, when the platform supplies it. */
    public fun mslAltitude(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<Float>> =
        location(interval, accuracy).mapNotNull { reading ->
            reading.value.mslAltitudeMeters?.let { Reading(it.toFloat(), reading.time) }
        }

    public fun mslAltimeter(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): FlowAltimeter =
        FlowAltimeter(mslAltitude(interval, accuracy), scope)

    /** GNSS satellite details where the platform provides them. */
    public fun satelliteStatus(): Flow<Reading<List<Satellite>>> = platformSatelliteStatus()

    public fun satelliteStatusSensor(scope: CoroutineScope): FlowSatelliteStatus = FlowSatelliteStatus(satelliteStatus(), scope)

    /** Monitor circular geofences while location is being collected. */
    public fun geofenceTransitions(
        fences: List<Geofence>,
        interval: Duration = 1.seconds,
        accuracy: SensorAccuracy = SensorAccuracy.Balanced,
    ): Flow<GeofenceTransition> = location(interval, accuracy).geofenceTransitions(fences)

    public fun gps(scope: CoroutineScope, interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): FlowGPS =
        FlowGPS(location(interval, accuracy), scope)

    public fun location(interval: Duration = 1.seconds, accuracy: SensorAccuracy = SensorAccuracy.Balanced): Flow<Reading<LocationReading>> =
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
