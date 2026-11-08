package com.kylecorry.andromeda.sense.orientation

import com.kylecorry.andromeda.device.DeviceSensors
import com.kylecorry.andromeda.device.SensorAccuracy
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** Tilt from gravity; heading cannot be measured without a magnetic reference. */
fun DeviceSensors.tiltSensor(
    scope: CoroutineScope,
    interval: Duration = 20.milliseconds,
    accuracy: SensorAccuracy = SensorAccuracy.Balanced,
): GravityRotationSensor = GravityRotationSensor(gravitySensor(scope, interval, accuracy))

/** Magnetic attitude derived from the gravity and magnetic-field sensors. */
fun DeviceSensors.geomagneticOrientationSensor(
    scope: CoroutineScope,
    interval: Duration = 20.milliseconds,
    accuracy: SensorAccuracy = SensorAccuracy.Balanced,
): CustomGeomagneticRotationSensor = CustomGeomagneticRotationSensor(
    magnetometer(scope, interval, accuracy), gravitySensor(scope, interval, accuracy)
)

/** Magnetic attitude stabilized with angular velocity from the gyroscope. */
fun DeviceSensors.fusedOrientationSensor(
    scope: CoroutineScope,
    interval: Duration = 20.milliseconds,
    accuracy: SensorAccuracy = SensorAccuracy.Balanced,
): CustomRotationSensor = CustomRotationSensor(
    magnetometer(scope, interval, accuracy), gravitySensor(scope, interval, accuracy),
    gyroscope(scope, interval, accuracy), scope
)
