package com.kylecorry.trail_sense.shared.extensions

import android.os.Bundle
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.sensors.ISpeedometer
import com.kylecorry.andromeda.core.ui.ReactiveComponent
import com.kylecorry.andromeda.core.ui.useCallback
import com.kylecorry.andromeda.core.ui.useService
import com.kylecorry.andromeda.fragments.ReactiveAndromedaFragment
import com.kylecorry.andromeda.fragments.onBackPressed
import com.kylecorry.andromeda.fragments.useBackgroundEffect
import com.kylecorry.andromeda.fragments.useTopic
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.signal.ICellSignalSensor
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.TimerActionBehavior
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import com.kylecorry.sol.units.Weight
import com.kylecorry.sol.units.WeightUnits
import com.kylecorry.trail_sense.main.MainActivity
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.ILoadingIndicator
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.NavigationSensorValues
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.time.Duration
import java.util.UUID
import kotlin.coroutines.CoroutineContext

// Sensors

fun ReactiveComponent.useGPSSensor(
    frequency: Duration = SensorService.DEFAULT_GPS_FREQUENCY,
    tag: String? = null
): IGPS {
    val sensors = useService<SensorService>()
    return useMemo(sensors, frequency.seconds, frequency.nano, tag) { sensors.getGPS(frequency, tag = tag) }
}

fun ReactiveComponent.useCellSignalSensor(
    removeUnregisteredSignals: Boolean = true,
    vararg values: Any?
): ICellSignalSensor {
    val sensors = useService<SensorService>()
    return useMemo(sensors, removeUnregisteredSignals, *values) {
        sensors.getCellSignal(
            removeUnregisteredSignals
        )
    }
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

// Common sensor readings
fun ReactiveComponent.useGPSLocation(
    frequency: Duration = SensorService.DEFAULT_GPS_FREQUENCY
): Pair<Coordinate, Float?> {
    val gps = useGPSSensor(frequency, this::class.java.simpleName)
    return useTopic(gps, gps.location to gps.horizontalAccuracy) {
        it.location to it.horizontalAccuracy
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

fun ReactiveComponent.useLocation(refreshPolicy: SensorSubsystem.SensorRefreshPolicy = SensorSubsystem.SensorRefreshPolicy.RefreshIfInvalid): Pair<Coordinate, Boolean> {
    val sensors = useService<SensorSubsystem>()
    val lastLocation = useMemo(sensors) {
        sensors.lastKnownLocation
    }
    val (location, setLocation) = useState(lastLocation)
    val (isUpToDate, setIsUpToDate) = useState(refreshPolicy == SensorSubsystem.SensorRefreshPolicy.Cache)

    useBackgroundEffect(refreshPolicy, cancelWhenRerun = true) {
        setLocation(sensors.getLocation(refreshPolicy))
        setIsUpToDate(true)
    }

    return location to isUpToDate
}

fun ReactiveComponent.useMainActivity(): MainActivity {
    return useActivity() as MainActivity
}

fun <T> T.useBottomSheetBackPressedCallback(
    vararg values: Any?,
    callback: OnBackPressedCallback.() -> Boolean
) where T : BottomSheetDialogFragment, T : ReactiveComponent {
    val listenerRef = useRef<OnBackPressedCallback?>(null)
    useEffect(*values) {
        listenerRef.current?.remove()
        listenerRef.current = null
        val dispatcher = (dialog as? ComponentDialog)?.onBackPressedDispatcher
            ?: return@useEffect
        val listener = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val consumed = callback()
                if (!consumed) {
                    isEnabled = false
                    dispatcher.onBackPressed()
                }
            }
        }
        dispatcher.addCallback(viewLifecycleOwner, listener)
        listenerRef.current = listener
    }
    useDestroyEffect {
        listenerRef.current?.remove()
    }
}

fun ReactiveAndromedaFragment.useShowDisclaimer(
    title: String,
    message: CharSequence,
    shownKey: String,
    okText: String = getString(android.R.string.ok),
    cancelText: String? = getString(android.R.string.cancel),
    considerShownIfCancelled: Boolean = true,
    shownValue: Boolean = true,
    onClose: (cancelled: Boolean, agreed: Boolean) -> Unit = { _, _ -> }
) {
    val context = useAndroidContext()
    useEffect {
        CustomUiUtils.disclaimer(
            context,
            title,
            message,
            shownKey,
            okText,
            cancelText,
            considerShownIfCancelled,
            shownValue,
            onClose
        )
    }
}

fun ReactiveComponent.useLoadingIndicator(isLoading: Boolean, indicator: ILoadingIndicator) {
    useEffect(indicator, isLoading) {
        if (isLoading) {
            indicator.show()
        } else {
            indicator.hide()
        }
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

fun ReactiveComponent.useDistancePreference(
    key: String
): Pair<Distance?, (Distance?) -> Unit> {
    val (value, setValue) = useFloatPreference("$key-value")
    val (unit, setUnit) = useIntPreference("$key-unit")

    val setter = useCallback { distance: Distance? ->
        setValue(distance?.value)
        setUnit(distance?.units?.id)
    }

    val distance = useMemo(value, unit) {
        if (value == null || unit == null) {
            return@useMemo null
        }

        Distance.from(
            value,
            DistanceUnits.entries.firstOrNull { it.id == unit } ?: DistanceUnits.Meters)
    }

    return distance to setter
}

fun ReactiveComponent.useWeightPreference(
    key: String
): Pair<Weight?, (Weight?) -> Unit> {
    val (value, setValue) = useFloatPreference("$key-value")
    val (unit, setUnit) = useIntPreference("$key-unit")

    val setter = useCallback { weight: Weight? ->
        setValue(weight?.value)
        setUnit(weight?.units?.id)
    }

    val weight = useMemo(value, unit) {
        if (value == null || unit == null) {
            return@useMemo null
        }

        Weight.from(
            value,
            WeightUnits.entries.firstOrNull { it.id == unit } ?: WeightUnits.Kilograms)
    }

    return weight to setter
}

fun ReactiveComponent.useSpeedPreference(
    key: String
): Pair<Speed?, (Speed?) -> Unit> {
    val (value, setValue) = useFloatPreference("$key-value")
    val (distanceUnit, setDistanceUnit) = useIntPreference("$key-distance-unit")
    val (timeUnit, setTimeUnit) = useIntPreference("$key-time-unit")

    val setter = useCallback { speed: Speed? ->
        setValue(speed?.value)
        setDistanceUnit(speed?.distanceUnits?.id)
        setTimeUnit(speed?.timeUnits?.id)
    }

    val speed = useMemo(value, distanceUnit, timeUnit) {
        if (value == null || distanceUnit == null || timeUnit == null) {
            return@useMemo null
        }

        Speed.from(
            value,
            DistanceUnits.entries.firstOrNull { it.id == distanceUnit } ?: DistanceUnits.Meters,
            TimeUnits.entries.firstOrNull { it.id == timeUnit } ?: TimeUnits.Seconds
        )
    }

    return speed to setter
}

fun ReactiveComponent.useLifecycleEffect(
    lifecycleEvent: Lifecycle.Event,
    vararg values: Any?,
    action: () -> Unit
) {
    val owner = useLifecycleOwner()
    val observer = useMemo(*values, lifecycleEvent) {
        LifecycleEventObserver { source: LifecycleOwner, event: Lifecycle.Event ->
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
    useLifecycleEffect(
        Lifecycle.Event.ON_DESTROY,
        *values
    ) {
        action()
    }
}

fun ReactiveComponent.usePauseEffect(vararg values: Any?, action: () -> Unit) {
    useLifecycleEffect(
        Lifecycle.Event.ON_PAUSE,
        *values
    ) {
        action()
    }
}

fun ReactiveComponent.useResumeEffect(vararg values: Any?, action: () -> Unit) {
    useLifecycleEffect(
        Lifecycle.Event.ON_RESUME,
        *values
    ) {
        action()
    }
}


// LiveData
fun <T : Any, V> ReactiveComponent.useLiveData(
    data: LiveData<T>,
    default: V,
    mapper: (T) -> V
): V {
    val (state, setState) = useState(default)
    val owner = useLifecycleOwner()

    // Note: This does not change when the mapper changes
    useEffect(data, owner) {
        data.observe(owner) {
            setState(mapper(it))
        }
    }

    return state
}

fun <T : Any, V> ReactiveComponent.useLiveData(
    data: LiveData<T>,
    mapper: (T) -> V?
): V? {
    return useLiveData(data, null, mapper)
}

fun ReactiveComponent.useTimer(
    interval: Long,
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    observeOn: CoroutineContext = Dispatchers.Main,
    actionBehavior: TimerActionBehavior = TimerActionBehavior.Wait,
    runnable: suspend () -> Unit
) {
    val runnableRef = useRef(runnable)
    runnableRef.current = runnable

    val timer = useMemo {
        CoroutineTimer(scope, observeOn, actionBehavior) {
            runnableRef.current()
        }
    }

    useResumeEffect(timer, interval) {
        timer.interval(interval)
    }

    usePauseEffect(timer) {
        timer.stop()
    }
}

fun ReactiveComponent.useTrigger(): Pair<String, () -> Unit> {
    val (key, setKey) = useState("")
    val trigger = useCallback<Unit> {
        setKey(UUID.randomUUID().toString())
    }
    return useMemo(key, trigger) { key to trigger }
}

fun ReactiveComponent.useToolEventListener(eventId: String, callback: (data: Bundle) -> Unit) {
    val callbackRef = useRef(callback)

    useEffect(callback) {
        callbackRef.current = callback
    }

    val subscriber: (Bundle) -> Unit = useMemo {
        { data ->
            callbackRef.current(data)
        }
    }

    useResumeEffect(eventId) {
        Tools.subscribe(eventId, subscriber)
    }

    usePauseEffect(eventId) {
        Tools.unsubscribe(eventId, subscriber)
    }
}
