package com.kylecorry.trail_sense.shared.sensors

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.sensors.Sensor
import com.kylecorry.andromeda.sense.location.GpsSensor
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import kotlin.time.Duration

/**
 * [read] applied to this sensor, refreshed on each of its readings while the screen is at least [minActiveState].
 * The sensor only runs while the screen is in that state.
 */
@Composable
fun <T : Sensor, V> T.collectReadingAsState(
    minActiveState: Lifecycle.State = Lifecycle.State.RESUMED,
    read: (T) -> V
): State<V> {
    val sensor = this
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentRead by rememberUpdatedState(read)
    val reading = remember(sensor) { mutableStateOf(read(sensor)) }
    LaunchedEffect(sensor, lifecycle, minActiveState) {
        lifecycle.repeatOnLifecycle(minActiveState) {
            sensor.flow.collect { reading.value = currentRead(sensor) }
        }
    }
    return reading
}

/**
 * The user's location, elevation, speed and heading, read from the shared sensors while the screen is resumed.
 * With [trueNorth] the compass reports bearings corrected for the declination at the user's location.
 */
@Composable
fun rememberNavigationSensors(
    tag: String,
    gpsFrequency: Duration = SensorService.DEFAULT_GPS_FREQUENCY,
    trueNorth: Boolean = false,
    compassDelay: Int = SensorService.MOTION_SENSOR_DELAY
): NavigationSensorValues {
    val sensors = remember { DependencyRegistry.get<SensorService>() }
    val prefs = remember { DependencyRegistry.get<UserPreferences>() }
    val gps = remember(sensors, gpsFrequency, tag) { sensors.getGPS(gpsFrequency, tag = tag) }
    val compass = remember(sensors, compassDelay) { sensors.getCompass(delay = compassDelay) }
    val altimeter = remember(sensors, gps) { sensors.getAltimeter(gps = gps) }
    val speedometer = remember(sensors, gps) { sensors.getSpeedometer(gps = gps) }

    val gpsReading by gps.collectReadingAsState {
        Triple(it.location, it.horizontalAccuracy?.let { accuracy -> Distance.meters(accuracy) }, it.speed)
    }
    val speed by speedometer.collectReadingAsState { it.speed }
    val elevationReading by altimeter.collectReadingAsState {
        val verticalAccuracy = if (it is GpsSensor) it.verticalAccuracy else gps.verticalAccuracy
        Distance.meters(it.altitude) to verticalAccuracy?.let { accuracy -> Distance.meters(accuracy) }
    }
    val bearing by compass.collectReadingAsState { it.bearing }

    val (location, locationAccuracy, gpsSpeed) = gpsReading
    val (elevation, elevationAccuracy) = elevationReading
    val declinationStrategy = remember(prefs, gps) { DeclinationFactory().getDeclinationStrategy(prefs, gps) }
    val declination = remember(declinationStrategy, location) { declinationStrategy.getDeclination() }
    LaunchedEffect(compass, declination, trueNorth) {
        compass.declination = if (trueNorth) declination else 0f
    }

    return NavigationSensorValues(
        location,
        locationAccuracy,
        elevation,
        elevationAccuracy,
        bearing,
        declination,
        speed,
        gpsSpeed,
        gps,
        compass
    )
}
