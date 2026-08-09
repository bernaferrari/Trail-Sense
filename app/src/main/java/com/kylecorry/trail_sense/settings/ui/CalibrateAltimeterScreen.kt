@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import android.text.util.Linkify.WEB_URLS
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.CoroutineAlerts
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.cache.AppServiceRegistry
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.trail_sense.shared.extensions.asLiveData
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
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.DEMRepo
import com.kylecorry.trail_sense.shared.dem.DigitalElevationModelLoader
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlinx.coroutines.Dispatchers
import java.time.Duration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.observe

@Composable
fun CalibrateAltimeterToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { CalibrateAltimeterController(host) }
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

private class CalibrateAltimeterController(private val host: TrailSenseDestinationHost) {

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
    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }
    private val overridePopulationRunner = CoroutineQueueRunner(dispatcher = Dispatchers.Default)
    private val cache by lazy { PreferencesSubsystem.getInstance(host.requireContext()).preferences }

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
        prefs = UserPreferences(host.requireContext())
        sensorService = SensorService(host.requireContext())
        gps = CustomGPS(host.requireContext().applicationContext)
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

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = TrailSenseSpacing.lg,
                                end = TrailSenseSpacing.lg,
                                top = TrailSenseSpacing.lg,
                                bottom = TrailSenseSpacing.xxxl
                            ),
                            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xl)
                        ) {
                            item("title") {
                                Text(
                                    text = stringResource(R.string.pref_altimeter_calibration_title),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(
                                        top = TrailSenseSpacing.sm,
                                        bottom = TrailSenseSpacing.xs
                                    )
                                )
                            }

                            item("altitude_hero") {
                                AltimeterStatusHero(altitude = altitude)
                            }

                            item("source_section") {
                                AltimeterSection(title = stringResource(R.string.source)) {
                                    AltimeterChipPickerRow(
                                        icon = painterResource(R.drawable.ic_altitude),
                                        label = stringResource(R.string.source),
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
                                        }
                                    )
                                    if (conditionalState.canProvideOverrides) {
                                        TrailSenseDivider()
                                        AltimeterChipPickerRow(
                                            icon = painterResource(R.drawable.ic_recenter),
                                            label = stringResource(
                                                R.string.pref_elevation_override_title
                                            ),
                                            selected = overrideSummary,
                                            onClick = ::pickAltitudeOverride
                                        )
                                        TrailSenseDivider()
                                        AltimeterChipActionRow(
                                            icon = painterResource(R.drawable.ic_location),
                                            label = stringResource(R.string.override_from_gps),
                                            onClick = ::setOverrideFromGPS
                                        )
                                        if (conditionalState.hasBarometer) {
                                            TrailSenseDivider()
                                            AltimeterChipActionRow(
                                                icon = painterResource(R.drawable.ic_barometer),
                                                label = stringResource(
                                                    R.string.elevation_from_pressure
                                                ),
                                                summary = stringResource(
                                                    R.string.pref_elevation_override_sea_level_title
                                                ),
                                                onClick = ::pickOverrideFromBarometer
                                            )
                                        }
                                    }
                                }
                            }

                            if (conditionalState.isFused) {
                                item("calibration_section") {
                                    AltimeterSection(
                                        title = forceCalibrationTitle
                                    ) {
                                        AltimeterChipPickerRow(
                                            icon = painterResource(R.drawable.ic_update),
                                            label = forceCalibrationTitle,
                                            selected = forceCalibrationSummary,
                                            onClick = {
                                                showForceCalibrationPicker = true
                                            }
                                        )
                                        TrailSenseDivider()
                                        AltimeterChipSwitchRow(
                                            icon = painterResource(R.drawable.refresh_altitude),
                                            label = stringResource(R.string.continuous_calibration),
                                            summary = stringResource(
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
                                            }
                                        )
                                        TrailSenseDivider()
                                        AltimeterChipActionRow(
                                            icon = painterResource(R.drawable.ic_delete),
                                            label = stringResource(R.string.reset_calibration),
                                            onClick = {
                                                FusedAltimeter.clearCachedCalibration(cache)
                                                host.toast(getString(R.string.done))
                                            }
                                        )
                                    }
                                }
                            }

                            item("data_section") {
                                AltimeterSection(
                                    title = stringResource(
                                        R.string.plugin_digital_elevation_model
                                    )
                                ) {
                                    AltimeterChipActionRow(
                                        icon = painterResource(R.drawable.ic_layers),
                                        label = stringResource(
                                            R.string.plugin_digital_elevation_model
                                        ),
                                        summary = demSummary,
                                        onClick = ::loadDEM
                                    )
                                    if (conditionalState.clearDemVisible) {
                                        TrailSenseDivider()
                                        AltimeterChipActionRow(
                                            icon = painterResource(R.drawable.ic_delete),
                                            label = stringResource(R.string.remove_dem),
                                            onClick = ::clearDEM
                                        )
                                    }
                                    if (conditionalState.showSamples) {
                                        TrailSenseDivider()
                                        AltimeterChipPickerRow(
                                            icon = painterResource(R.drawable.satellite),
                                            label = stringResource(R.string.samples),
                                            selected = samples.toString(),
                                            onClick = {
                                                pickSamples {
                                                    samples = it
                                                }
                                            }
                                        )
                                    }
                                    TrailSenseDivider()
                                    AltimeterChipSwitchRow(
                                        icon = painterResource(R.drawable.satellite),
                                        label = stringResource(
                                            R.string.pref_nmea_elevation_title
                                        ),
                                        summary = stringResource(
                                            R.string.pref_nmea_elevation_summary
                                        ),
                                        checked = useNmea,
                                        onCheckedChange = {
                                            useNmea = it
                                            putBoolean(R.string.pref_nmea_altitude, it)
                                            restartAltimeter()
                                        }
                                    )
                                }
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

    private fun onViewCreated() {
        val seaLevelPressureKey = getString(R.string.pref_sea_level_pressure_override)
        cache.onChange.asLiveData().observe(host.lifecycleOwner) {
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
        host.inBackground {
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
                        host.toast(getString(R.string.elevation_override_updated_toast))
                    }
                }
            }
        }
    }

    private fun setOverrideFromGPS() {
        host.inBackground {
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
                        host.toast(getString(R.string.elevation_override_updated_toast))
                    }
                }
            }
        }
    }

    private fun updateAltitudeOverride() {
        host.inBackground {
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
            host.requireContext(),
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
            host.requireContext(),
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
            host.requireContext(),
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
        host.inBackground {
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
        host.inBackground(state = BackgroundMinimumState.Created) {
            val instructions = getString(
                R.string.digital_elevation_model_link,
                URLs.DEM
            ).toSpannable()
            LinkifyCompat.addLinks(instructions, WEB_URLS)
            val cancelled = CoroutineAlerts.dialog(
                host.requireContext(),
                getString(R.string.plugin_digital_elevation_model),
                instructions,
                allowLinks = true
            )

            if (cancelled) {
                return@inBackground
            }

            val source = IntentUriPicker(
                host.intentResultRetriever,
                host.requireContext()
            ).open(listOf("application/zip")) ?: return@inBackground

            val loader = DigitalElevationModelLoader()
            try {
                Alerts.withProgress(host.requireContext(), getString(R.string.loading)) { setProgress ->
                    loader.load(source).collect(setProgress)
                    onMain {
                        onAltimeterModeChanged()
                        host.toast(getString(R.string.loaded))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onMain {
                    host.toast(getString(R.string.invalid_dem_file))
                }
            }
        }
    }

    private fun clearDEM() {
        host.inBackground {
            val cancelled = CoroutineAlerts.dialog(host.requireContext(), getString(R.string.remove_dem))
            if (cancelled) {
                return@inBackground
            }
            val loader = DigitalElevationModelLoader()
            Alerts.withLoading(host.requireContext(), getString(R.string.loading)) {
                loader.clear()
                onMain {
                    onAltimeterModeChanged()
                    host.toast(getString(R.string.done))
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
        host.inBackground {
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
            host.requireContext(),
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
        return host.getString(id, *args)
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

@Composable
private fun AltimeterStatusHero(
    altitude: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(TrailSenseSpacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_altitude),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.width(TrailSenseSpacing.lg))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
            ) {
                Text(
                    text = stringResource(R.string.elevation),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    text = altitude,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun AltimeterIconChip(icon: Painter) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun AltimeterChipActionRow(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                horizontal = TrailSenseSpacing.lg,
                vertical = TrailSenseSpacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AltimeterIconChip(icon)
        Spacer(Modifier.width(TrailSenseSpacing.lg))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(TrailSenseSpacing.md))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AltimeterChipPickerRow(
    icon: Painter,
    label: String,
    selected: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    AltimeterChipActionRow(
        icon = icon,
        label = label,
        summary = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    )
}

@Composable
private fun AltimeterChipSwitchRow(
    icon: Painter,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .padding(
                horizontal = TrailSenseSpacing.lg,
                vertical = TrailSenseSpacing.md
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AltimeterIconChip(icon)
        Spacer(Modifier.width(TrailSenseSpacing.lg))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(TrailSenseSpacing.md))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun AltimeterSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(
                start = TrailSenseSpacing.md,
                bottom = TrailSenseSpacing.sm
            )
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(modifier = Modifier.padding(vertical = TrailSenseSpacing.xs)) {
                content()
            }
        }
    }
}
