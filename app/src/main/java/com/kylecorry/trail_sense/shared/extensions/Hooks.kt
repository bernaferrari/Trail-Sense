package com.kylecorry.trail_sense.shared.extensions

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.NavigationSensorValues
import com.kylecorry.trail_sense.shared.sensors.SensorService
import java.time.Duration

// Sensors

fun ReactiveComponent.useGPSSensor(
    frequency: Duration = SensorService.DEFAULT_GPS_FREQUENCY,
    tag: String? = null
): IGPS {
    val sensors = useService<SensorService>()
    return useMemo(sensors, frequency.seconds, frequency.nano, tag) { sensors.getGPS(frequency, tag = tag) }
}

fun ReactiveComponent.useCompassSensor(delay: Int = SensorService.MOTION_SENSOR_DELAY): ICompass {
    val sensors = useService<SensorService>()
    return useMemo(sensors, delay) { sensors.getCompass(delay = delay) }
}

fun ReactiveComponent.useAltimeterSensor(gps: IGPS? = null): IAltimeter {
    val sensors = useService<SensorService>()
    return useMemo(sensors, gps) {
        sensors.getAltimeter(gps = gps)
    }
}

fun ReactiveComponent.useSpeedometerSensor(gps: IGPS? = null): ISpeedometer {
    val sensors = useService<SensorService>()
    return useMemo(sensors, gps) {
        sensors.getSpeedometer(gps = gps)
    }
}

fun ReactiveComponent.useNavigationSensors(
    gpsFrequency: Duration = SensorService.DEFAULT_GPS_FREQUENCY,
    trueNorth: Boolean = false,
    compassDelay: Int = SensorService.MOTION_SENSOR_DELAY
): NavigationSensorValues {
    val gps = useGPSSensor(gpsFrequency, this::class.java.simpleName)
    val compass = useCompassSensor(compassDelay)
    val altimeter = useAltimeterSensor(gps)
    val speedometer = useSpeedometerSensor(gps)
    val prefs = useService<UserPreferences>()

    val defaultGpsReading = useMemo(gps) {
        Triple(gps.location, gps.horizontalAccuracy?.let { Distance.meters(it) }, gps.speed)
    }

    val defaultSpeedReading = useMemo(speedometer) {
        speedometer.speed
    }

    val defaultElevationReading = useMemo(altimeter, gps) {
        Distance.meters(altimeter.altitude) to (if (altimeter is IGPS) altimeter.verticalAccuracy else gps.verticalAccuracy)?.let {
            Distance.meters(
                it
            )
        }
    }

    val defaultCompassReading = useMemo(compass) {
        compass.bearing
    }

    val declinationProvider = useMemo(prefs, gps) {
        DeclinationFactory().getDeclinationStrategy(prefs, gps)
    }
    val declination = useMemo(gps.location) { declinationProvider.getDeclination() }
    useEffect(compass, declination) { compass.declination = if (trueNorth) declination else 0f }

    val (location, locationAccuracy, gpsSpeed) = useTopic(
        gps,
        defaultGpsReading
    ) {
        Triple(gps.location, gps.horizontalAccuracy?.let { Distance.meters(it) }, gps.speed)
    }

    val speed = useTopic(speedometer, defaultSpeedReading) {
        speedometer.speed
    }

    val (elevation, elevationAccuracy) = useTopic(
        altimeter,
        defaultElevationReading
    ) {
        Distance.meters(altimeter.altitude) to (if (altimeter is IGPS) altimeter.verticalAccuracy else gps.verticalAccuracy)?.let {
            Distance.meters(
                it
            )
        }
    }

    val bearing = useTopic(compass, defaultCompassReading) {
        compass.bearing
    }

    return useMemo(
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
    ) {
        NavigationSensorValues(
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
}

// Preferences

fun <T> ReactiveComponent.usePreference(
    key: String,
    load: (IPreferences, String) -> T?,
    save: (IPreferences, String, T) -> Unit
): Pair<T?, (T?) -> Unit> {
    val prefs = useService<PreferencesSubsystem>()

    val initialValue = useMemo(prefs, key) {
        load(prefs.preferences, key)
    }

    val (value, setValue) = useState(initialValue)

    val updatePreference = useCallback(key, prefs) { newValue: T? ->
        if (newValue == null) {
            prefs.preferences.remove(key)
        } else {
            save(prefs.preferences, key, newValue)
        }
        setValue(newValue)
    }

    return value to updatePreference
}

fun ReactiveComponent.useFloatPreference(
    key: String
): Pair<Float?, (Float?) -> Unit> {
    return usePreference(key, IPreferences::getFloat, IPreferences::putFloat)
}

fun ReactiveComponent.useIntPreference(
    key: String
): Pair<Int?, (Int?) -> Unit> {
    return usePreference(key, IPreferences::getInt, IPreferences::putInt)
}

fun ReactiveComponent.useCoordinatePreference(
    key: String
): Pair<Coordinate?, (Coordinate?) -> Unit> {
    return usePreference(key, IPreferences::getCoordinate, IPreferences::putCoordinate)
}

// Lifecycle

fun ReactiveComponent.useLifecycleEffect(
    lifecycleEvent: Lifecycle.Event,
    vararg values: Any?,
    action: () -> Unit
) {
    val owner = useLifecycleOwner()
    val observer = useMemo(*values, lifecycleEvent) {
        LifecycleEventObserver { _: LifecycleOwner, event: Lifecycle.Event ->
            if (event == lifecycleEvent) {
                action()
            }
        }
    }

    useEffectWithCleanup(arrayOf(owner, observer)) {
        owner.lifecycle.addObserver(observer)
        return@useEffectWithCleanup {
            owner.lifecycle.removeObserver(observer)
        }
    }
}

fun ReactiveComponent.useDestroyEffect(vararg values: Any?, action: () -> Unit) {
    useLifecycleEffect(Lifecycle.Event.ON_DESTROY, *values) {
        action()
    }
}

fun ReactiveComponent.usePauseEffect(vararg values: Any?, action: () -> Unit) {
    useLifecycleEffect(Lifecycle.Event.ON_PAUSE, *values) {
        action()
    }
}
