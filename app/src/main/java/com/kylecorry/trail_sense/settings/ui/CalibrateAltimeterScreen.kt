@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import android.text.util.Linkify.WEB_URLS
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeDestinationFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.fragments.asLiveData
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.andromeda.sense.altitude.FusedAltimeter
import com.kylecorry.andromeda.sense.barometer.IBarometer
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.time.CoroutineTimer
import com.kylecorry.luna.time.Throttle
import com.kylecorry.sol.science.meteorology.Meteorology
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Pressure
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.URLs
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.andromeda_temp.getOrNull
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.DEMRepo
import com.kylecorry.trail_sense.shared.dem.DigitalElevationModelLoader
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlinx.coroutines.Dispatchers
import java.time.Duration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun CalibrateAltimeterToolScreen(fragment: TrailSenseComposeDestinationFragment) {
    val controller = remember(fragment) { CalibrateAltimeterController(fragment) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onCreate()
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onDestroy()
        }
    }

    controller.Content()
}

private class CalibrateAltimeterController(private val fragment: TrailSenseComposeDestinationFragment) {

    private lateinit var barometer: IBarometer
    private lateinit var gps: IGPS
    private lateinit var altimeter: IAltimeter
    private lateinit var prefs: UserPreferences
    private lateinit var sensorService: SensorService
    private val throttle = Throttle(20)
    private var altimeterStarted = false
    private lateinit var distanceUnits: DistanceUnits

    private lateinit var lastMode: UserPreferences.AltimeterMode
    private val updateTimer = CoroutineTimer { updateAltitude() }
    private val formatService by lazy { FormatService.getInstance(fragment.requireContext()) }
    private val overridePopulationRunner = CoroutineQueueRunner(dispatcher = Dispatchers.Default)
    private val cache by lazy { PreferencesSubsystem.getInstance(fragment.requireContext()).preferences }

    private var setAltitudeState: ((String) -> Unit)? = null
    private var setModeState: ((String) -> Unit)? = null
    private var setOverrideSummary: ((String) -> Unit)? = null
    private var setSamplesState: ((Int) -> Unit)? = null
    private var setContinuousCalibrationState: ((Boolean) -> Unit)? = null
    private var setNmeaState: ((Boolean) -> Unit)? = null
    private var setForceCalibrationSummary: ((String) -> Unit)? = null
    private var setConditionalState: ((AltimeterConditionalState) -> Unit)? = null
    private var setDemSummary: ((String) -> Unit)? = null

    fun onCreate() {
        prefs = UserPreferences(fragment.requireContext())
        sensorService = SensorService(fragment.requireContext())
        gps = CustomGPS(fragment.requireContext().applicationContext)
        barometer = sensorService.getBarometer()
        altimeter = sensorService.getAltimeter()
        distanceUnits = prefs.baseDistanceUnits
        ensureDefaultMode()
        lastMode = prefs.altimeterMode
        onViewCreated()
    }

    @androidx.compose.runtime.Composable
    fun Content() {
                    val modeOptions = getCalibrationModeOptions()

                    var altitude by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(getString(R.string.dash))
                    }
                    var mode by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(getModePreference())
                    }
                    var overrideSummary by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(formatAltitudeOverride())
                    }
                    var samples by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(prefs.altimeterSamples)
                    }
                    var continuousCalibration by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(getBoolean(
                            R.string.pref_altimeter_continuous_calibration,
                            true
                        ))
                    }
                    var useNmea by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(getBoolean(
                            R.string.pref_nmea_altitude,
                            false
                        ))
                    }
                    var forceCalibrationSummary by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(formatForceCalibrationInterval())
                    }
                    var conditionalState by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(getConditionalState())
                    }
                    var demSummary by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(getString(R.string.built_in_dem))
                    }
                    var showForceCalibrationPicker by androidx.compose.runtime.remember {
                        androidx.compose.runtime.mutableStateOf(false)
                    }
                    val forceCalibrationTitle = androidx.compose.ui.res.stringResource(
                        R.string.fused_altimeter_force_calibration
                    )

                    setAltitudeState = { altitude = it }
                    setModeState = { mode = it }
                    setOverrideSummary = { overrideSummary = it }
                    setSamplesState = { samples = it }
                    setContinuousCalibrationState = { continuousCalibration = it }
                    setNmeaState = { useNmea = it }
                    setForceCalibrationSummary = { forceCalibrationSummary = it }
                    setConditionalState = { conditionalState = it }
                    setDemSummary = { demSummary = it }

                    androidx.compose.material3.Surface(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = TrailSenseSpacing.lg,
                                end = TrailSenseSpacing.lg,
                                top = TrailSenseSpacing.lg,
                                bottom = TrailSenseSpacing.xxxl
                            )
                        ) {
                            item("header") {
                                TrailSenseSectionHeader(
                                    title = androidx.compose.ui.res.stringResource(
                                        R.string.pref_altimeter_calibration_title
                                    )
                                )
                            }
                            item("altitude") {
                                SettingsInfoRow(
                                    label = androidx.compose.ui.res.stringResource(
                                        R.string.elevation
                                    ),
                                    summary = altitude,
                                    modifier = androidx.compose.ui.Modifier.padding(
                                        horizontal = TrailSenseSpacing.lg
                                    )
                                )
                            }
                            item("mode") {
                                SettingsPickerRow(
                                    label = androidx.compose.ui.res.stringResource(R.string.source),
                                    selected = optionLabel(modeOptions, mode),
                                    onClick = {
                                        pickOption(
                                            getString(R.string.source),
                                            modeOptions,
                                            mode
                                        ) {
                                            putString(
                                                R.string.pref_altimeter_calibration_mode,
                                                it
                                            )
                                            mode = it
                                            onAltimeterModeChanged()
                                        }
                                    },
                                    modifier = androidx.compose.ui.Modifier.padding(
                                        horizontal = TrailSenseSpacing.lg
                                    )
                                )
                            }
                            if (conditionalState.canProvideOverrides) {
                                item("override") {
                                    SettingsActionRow(
                                        label = androidx.compose.ui.res.stringResource(
                                            R.string.pref_elevation_override_title
                                        ),
                                        summary = overrideSummary,
                                        onClick = ::pickAltitudeOverride,
                                        modifier = androidx.compose.ui.Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                                }
                                item("override_gps") {
                                    SettingsActionRow(
                                        label = androidx.compose.ui.res.stringResource(
                                            R.string.override_from_gps
                                        ),
                                        onClick = ::setOverrideFromGPS,
                                        modifier = androidx.compose.ui.Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                                }
                                if (conditionalState.hasBarometer) {
                                    item("override_barometer") {
                                        SettingsActionRow(
                                            label = androidx.compose.ui.res.stringResource(
                                                R.string.elevation_from_pressure
                                            ),
                                            summary = androidx.compose.ui.res.stringResource(
                                                R.string.pref_elevation_override_sea_level_title
                                            ),
                                            onClick = ::pickOverrideFromBarometer,
                                            modifier = androidx.compose.ui.Modifier.padding(
                                                horizontal = TrailSenseSpacing.lg
                                            )
                                        )
                                    }
                                }
                            }
                            if (conditionalState.isFused) {
                                item("force_calibration") {
                                    SettingsActionRow(
                                        label = forceCalibrationTitle,
                                        summary = forceCalibrationSummary,
                                        onClick = {
                                            showForceCalibrationPicker = true
                                        },
                                        modifier = androidx.compose.ui.Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                                }
                                item("continuous_calibration") {
                                    SettingsSwitchRow(
                                        label = androidx.compose.ui.res.stringResource(
                                            R.string.continuous_calibration
                                        ),
                                        summary = androidx.compose.ui.res.stringResource(
                                            R.string.continuous_calibration_description
                                        ),
                                        checked = continuousCalibration,
                                        onCheckedChange = {
                                            continuousCalibration = it
                                            putBoolean(
                                                R.string.pref_altimeter_continuous_calibration,
                                                it
                                            )
                                            restartAltimeter()
                                        },
                                        modifier = androidx.compose.ui.Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                                    TrailSenseDivider()
                                }
                            }
                            item("dem") {
                                SettingsActionRow(
                                    label = androidx.compose.ui.res.stringResource(
                                        R.string.plugin_digital_elevation_model
                                    ),
                                    summary = demSummary,
                                    onClick = ::loadDEM,
                                    modifier = androidx.compose.ui.Modifier.padding(
                                        horizontal = TrailSenseSpacing.lg
                                    )
                                )
                            }
                            if (conditionalState.clearDemVisible) {
                                item("clear_dem") {
                                    SettingsActionRow(
                                        label = androidx.compose.ui.res.stringResource(
                                            R.string.remove_dem
                                        ),
                                        onClick = ::clearDEM,
                                        modifier = androidx.compose.ui.Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                                }
                            }
                            if (conditionalState.showSamples) {
                                item("samples") {
                                    SettingsPickerRow(
                                        label = androidx.compose.ui.res.stringResource(
                                            R.string.samples
                                        ),
                                        selected = samples.toString(),
                                        onClick = {
                                            pickSamples {
                                                samples = it
                                            }
                                        },
                                        modifier = androidx.compose.ui.Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                                }
                            }
                            item("nmea") {
                                SettingsSwitchRow(
                                    label = androidx.compose.ui.res.stringResource(
                                        R.string.pref_nmea_elevation_title
                                    ),
                                    summary = androidx.compose.ui.res.stringResource(
                                        R.string.pref_nmea_elevation_summary
                                    ),
                                    checked = useNmea,
                                    onCheckedChange = {
                                        useNmea = it
                                        putBoolean(R.string.pref_nmea_altitude, it)
                                        restartAltimeter()
                                    },
                                    modifier = androidx.compose.ui.Modifier.padding(
                                        horizontal = TrailSenseSpacing.lg
                                    )
                                )
                                TrailSenseDivider()
                            }
                            if (conditionalState.isFused) {
                                item("clear_cache") {
                                    SettingsActionRow(
                                        label = androidx.compose.ui.res.stringResource(
                                            R.string.reset_calibration
                                        ),
                                        onClick = {
                                            FusedAltimeter.clearCachedCalibration(cache)
                                            fragment.toast(getString(R.string.done))
                                        },
                                        modifier = androidx.compose.ui.Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                            }
                        }
                    }

                    if (showForceCalibrationPicker) {
                        DurationPickerDialog(
                            title = forceCalibrationTitle,
                            duration = prefs.altimeter.fusedAltimeterForcedRecalibrationInterval,
                            onDurationPicked = {
                                if (it != null) {
                                    prefs.altimeter.fusedAltimeterForcedRecalibrationInterval = it
                                    forceCalibrationSummary = formatForceCalibrationInterval()
                                }
                                showForceCalibrationPicker = false
                            },
                            onDismiss = {
                                showForceCalibrationPicker = false
                            }
                        )
                    }
                }
    }

    private fun onViewCreated() {
        val seaLevelPressureKey = getString(R.string.pref_sea_level_pressure_override)
        cache.onChange.asLiveData().observe(fragment.viewLifecycleOwner) {
            if (it == seaLevelPressureKey) {
                restartAltimeter()
            }
        }
        refreshConditionalState()
    }

    fun onResume() {
        startAltimeter()
        updateTimer.interval(200)
    }

    fun onPause() {
        stopAltimeter()
        updateTimer.stop()
        overridePopulationRunner.cancel()
    }

    fun onDestroy() {
        setAltitudeState = null
        setModeState = null
        setOverrideSummary = null
        setSamplesState = null
        setContinuousCalibrationState = null
        setNmeaState = null
        setForceCalibrationSummary = null
        setConditionalState = null
        setDemSummary = null
    }

    private fun startAltimeter() {
        if (altimeterStarted) {
            return
        }
        altimeterStarted = true
        altimeter.start(this::updateAltitude)
    }

    private fun stopAltimeter() {
        altimeterStarted = false
        altimeter.stop(this::updateAltitude)
    }

    private fun restartAltimeter() {
        stopAltimeter()
        altimeter = sensorService.getAltimeter()
        startAltimeter()
    }

    private fun setOverrideFromBarometer(seaLevelPressure: Float) {
        fragment.inBackground {
            onDefault {
                overridePopulationRunner.replace {
                    barometer.read()
                    val elevation = Meteorology.getAltitude(
                        Pressure.hpa(barometer.pressure),
                        Pressure.hpa(seaLevelPressure)
                    )

                    onIO {
                        prefs.altitudeOverride = elevation.meters().value
                        prefs.seaLevelPressureOverride = seaLevelPressure
                    }

                    onMain {
                        refreshOverrideSummary()
                        restartAltimeter()
                        fragment.toast(getString(R.string.elevation_override_updated_toast))
                    }
                }
            }
        }
    }

    private fun setOverrideFromGPS() {
        fragment.inBackground {
            onDefault {
                overridePopulationRunner.replace {
                    readAll(listOf(gps, barometer))
                    val elevation = gps.altitude
                    val seaLevelPressure = Meteorology.getSeaLevelPressure(
                        Pressure.hpa(barometer.pressure),
                        Distance.meters(elevation)
                    )

                    onIO {
                        prefs.altitudeOverride = elevation
                        prefs.seaLevelPressureOverride = seaLevelPressure.value
                    }

                    onMain {
                        refreshOverrideSummary()
                        restartAltimeter()
                        fragment.toast(getString(R.string.elevation_override_updated_toast))
                    }
                }
            }
        }
    }

    private fun updateAltitudeOverride() {
        fragment.inBackground {
            onDefault {
                overridePopulationRunner.replace {
                    altimeter.read()
                    prefs.altitudeOverride = altimeter.altitude
                    onMain {
                        refreshOverrideSummary()
                    }
                }
            }
        }
    }

    private fun onAltimeterModeChanged() {
        lastMode = prefs.altimeterMode
        setModeState?.invoke(getModePreference())
        restartAltimeter()
        refreshConditionalState()

        if (prefs.altimeterMode == UserPreferences.AltimeterMode.Barometer) {
            updateAltitudeOverride()
        }
    }

    private fun updateAltitude(): Boolean {
        if (throttle.isThrottled()) {
            return true
        }

        val altitude = Distance.meters(altimeter.altitude).convertTo(distanceUnits)
        setAltitudeState?.invoke(formatService.formatDistance(altitude))

        if (lastMode != prefs.altimeterMode) {
            onAltimeterModeChanged()
        }

        refreshOverrideSummary()

        return true
    }

    private fun pickAltitudeOverride() {
        CustomUiUtils.pickElevation(
            fragment.requireContext(),
            Distance.meters(prefs.altitudeOverride).convertTo(distanceUnits),
            getString(R.string.pref_elevation_override_title)
        ) { elevation, _ ->
            if (elevation != null) {
                setAltitudeOverride(elevation)
            }
        }
    }

    private fun pickOverrideFromBarometer() {
        Pickers.number(
            fragment.requireContext(),
            getString(R.string.pref_elevation_override_sea_level_dialog_title),
            getString(R.string.pref_elevation_override_sea_level_dialog_msg),
            prefs.seaLevelPressureOverride,
            allowDecimals = true,
            allowNegative = false,
            hint = getString(R.string.pressure)
        ) {
            if (it != null) {
                setOverrideFromBarometer(it.toFloat())
            }
        }
    }

    private fun pickSamples(onPicked: (Int) -> Unit) {
        val samples = (1..8).toList()
        Pickers.item(
            fragment.requireContext(),
            getString(R.string.samples),
            samples.map { it.toString() },
            samples.indexOf(prefs.altimeterSamples).coerceAtLeast(0)
        ) {
            if (it != null) {
                prefs.altimeterSamples = samples[it]
                onPicked(samples[it])
                restartAltimeter()
            }
        }
    }

    private fun setAltitudeOverride(elevation: Distance) {
        fragment.inBackground {
            onDefault {
                overridePopulationRunner.replace {
                    if (prefs.altimeterMode == UserPreferences.AltimeterMode.Barometer) {
                        barometer.read()
                        val seaLevelPressure = Meteorology.getSeaLevelPressure(
                            Pressure.hpa(barometer.pressure),
                            elevation.meters()
                        )
                        prefs.altitudeOverride = elevation.meters().value
                        prefs.seaLevelPressureOverride = seaLevelPressure.value
                    } else {
                        prefs.altitudeOverride = elevation.meters().value
                    }

                    onMain {
                        refreshOverrideSummary()
                        restartAltimeter()
                    }
                }
            }
        }
    }

    private fun loadDEM() {
        fragment.inBackground(state = BackgroundMinimumState.Created) {
            val instructions = getString(
                R.string.digital_elevation_model_link,
                URLs.DEM
            ).toSpannable()
            LinkifyCompat.addLinks(instructions, WEB_URLS)
            val cancelled = CoroutineAlerts.dialog(
                fragment.requireContext(),
                getString(R.string.plugin_digital_elevation_model),
                instructions,
                allowLinks = true
            )

            if (cancelled) {
                return@inBackground
            }

            val source = IntentUriPicker(
                fragment.requireActivity() as IntentResultRetriever,
                fragment.requireContext()
            ).open(listOf("application/zip")) ?: return@inBackground

            val loader = DigitalElevationModelLoader()
            try {
                Alerts.withProgress(fragment.requireContext(), getString(R.string.loading)) { setProgress ->
                    loader.load(source).collect(setProgress)
                    onMain {
                        onAltimeterModeChanged()
                        fragment.toast(getString(R.string.loaded))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onMain {
                    fragment.toast(getString(R.string.invalid_dem_file))
                }
            }
        }
    }

    private fun clearDEM() {
        fragment.inBackground {
            val cancelled = CoroutineAlerts.dialog(fragment.requireContext(), getString(R.string.remove_dem))
            if (cancelled) {
                return@inBackground
            }
            val loader = DigitalElevationModelLoader()
            Alerts.withLoading(fragment.requireContext(), getString(R.string.loading)) {
                loader.clear()
                onMain {
                    onAltimeterModeChanged()
                    fragment.toast(getString(R.string.done))
                }
            }
        }
    }

    private fun refreshConditionalState() {
        setConditionalState?.invoke(getConditionalState())
        setForceCalibrationSummary?.invoke(formatForceCalibrationInterval())
        setContinuousCalibrationState?.invoke(
            getBoolean(R.string.pref_altimeter_continuous_calibration, true)
        )
        setNmeaState?.invoke(getBoolean(R.string.pref_nmea_altitude, false))
        updateDemSummary()
    }

    private fun updateDemSummary() {
        fragment.inBackground {
            val version = DEMRepo.getInstance().getVersion()
            onMain {
                setDemSummary?.invoke(version ?: getString(R.string.built_in_dem))
                setConditionalState?.invoke(getConditionalState())
            }
        }
    }

    private fun refreshOverrideSummary() {
        setOverrideSummary?.invoke(formatAltitudeOverride())
    }

    private fun formatAltitudeOverride(): String {
        val altitudeOverride = Distance.meters(prefs.altitudeOverride).convertTo(distanceUnits)
        return formatService.formatDistance(altitudeOverride)
    }

    private fun formatForceCalibrationInterval(): String {
        val interval =
            formatService.formatDuration(prefs.altimeter.fusedAltimeterForcedRecalibrationInterval)
        return getString(R.string.fused_altimeter_force_calibration_summary, interval)
    }

    private fun getConditionalState(): AltimeterConditionalState {
        val hasBarometer = prefs.weather.hasBarometer
        val mode = prefs.altimeterMode
        val isFused =
            hasBarometer && (mode == UserPreferences.AltimeterMode.GPSBarometer ||
                    mode == UserPreferences.AltimeterMode.DigitalElevationModelBarometer)
        val canProvideOverrides =
            mode == UserPreferences.AltimeterMode.Barometer ||
                    mode == UserPreferences.AltimeterMode.Override
        val showSamples =
            mode == UserPreferences.AltimeterMode.GPS ||
                    mode == UserPreferences.AltimeterMode.GPSBarometer

        return AltimeterConditionalState(
            hasBarometer = hasBarometer,
            isFused = isFused,
            canProvideOverrides = canProvideOverrides,
            showSamples = showSamples,
            clearDemVisible = DEM.isExternalModel()
        )
    }

    private fun getCalibrationModeOptions(): List<AltimeterOption> {
        val hasBarometer = prefs.weather.hasBarometer
        return listOfNotNull(
            if (hasBarometer) AltimeterOption(
                "gps_barometer",
                getString(R.string.altimeter_mode_gps_barometer)
            ) else null,
            AltimeterOption("gps", getString(R.string.gps)),
            if (hasBarometer) AltimeterOption(
                "barometer",
                getString(R.string.barometer)
            ) else null,
            if (hasBarometer) AltimeterOption(
                "dem_barometer",
                getString(R.string.altimeter_mode_dem_barometer)
            ) else null,
            AltimeterOption(
                "dem",
                getString(R.string.digital_elevation_model_abbreviation)
            ),
            AltimeterOption("override", getString(R.string.manual))
        )
    }

    private fun pickOption(
        title: String,
        options: List<AltimeterOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        Pickers.item(
            fragment.requireContext(),
            title,
            options.map { it.label },
            options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        ) {
            if (it != null) {
                onSelected(options[it].value)
            }
        }
    }

    private fun optionLabel(options: List<AltimeterOption>, selected: String): String {
        return options.firstOrNull { it.value == selected }?.label ?: selected
    }

    private fun ensureDefaultMode() {
        val key = getString(R.string.pref_altimeter_calibration_mode)
        if (AppServiceRegistry.get<PreferencesSubsystem>().preferences.getString(key) == null) {
            cache.putString(key, if (prefs.weather.hasBarometer) "gps_barometer" else "gps")
        }
    }

    private fun getModePreference(): String {
        return cache.getString(getString(R.string.pref_altimeter_calibration_mode))
            ?: if (prefs.weather.hasBarometer) "gps_barometer" else "gps"
    }

    private fun getBoolean(key: Int, defaultValue: Boolean = false): Boolean {
        return cache.getBoolean(getString(key)) ?: defaultValue
    }

    private fun putString(key: Int, value: String) {
        cache.putString(getString(key), value)
    }

    private fun putBoolean(key: Int, value: Boolean) {
        cache.putBoolean(getString(key), value)
    }

    private fun getString(@StringRes id: Int, vararg args: Any): String {
        return fragment.getString(id, *args)
    }

    private data class AltimeterConditionalState(
        val hasBarometer: Boolean,
        val isFused: Boolean,
        val canProvideOverrides: Boolean,
        val showSamples: Boolean,
        val clearDemVisible: Boolean
    )

    private data class AltimeterOption(
        val value: String,
        val label: String
    )
}
