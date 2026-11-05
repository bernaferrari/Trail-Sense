@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import android.content.Context
import android.text.util.Linkify.WEB_URLS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.text.toSpannable
import androidx.core.text.util.LinkifyCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.core.sensors.IAltimeter
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.andromeda.sense.altitude.FusedAltimeter
import com.kylecorry.andromeda.sense.barometer.IBarometer
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.andromeda.sense.readAll
import com.kylecorry.luna.concurrency.CoroutineQueueRunner
import com.kylecorry.luna.concurrency.onDefault
import com.kylecorry.luna.concurrency.onIO
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.luna.result.getOrNull
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
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.CoroutineAlerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.icons.trail.*
import com.kylecorry.trail_sense.shared.io.IntentUriPicker
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.CustomGPS
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Duration

@Composable
fun CalibrateAltimeterToolScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val viewModel = viewModel {
        CalibrateAltimeterViewModel(
            prefs = UserPreferences(appContext),
            cache = PreferencesSubsystem.getInstance(appContext).preferences,
            sensorService = SensorService(appContext),
            gps = CustomGPS(appContext),
            keys = AltimeterPreferenceKeys(
                mode = appContext.getString(R.string.pref_altimeter_calibration_mode),
                seaLevelPressureOverride = appContext.getString(R.string.pref_sea_level_pressure_override),
                continuousCalibration = appContext.getString(R.string.pref_altimeter_continuous_calibration),
                nmea = appContext.getString(R.string.pref_nmea_altitude)
            )
        )
    }
    val formatService = remember(appContext) { FormatService.getInstance(appContext) }

    LifecycleResumeEffect(viewModel) {
        viewModel.start()
        onPauseOrDispose {
            viewModel.stop()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect {
            effects.showMessage(
                context.getString(
                    when (it) {
                        AltimeterMessage.Done -> R.string.done
                        AltimeterMessage.OverrideUpdated -> R.string.elevation_override_updated_toast
                        AltimeterMessage.Loaded -> R.string.loaded
                        AltimeterMessage.InvalidDem -> R.string.invalid_dem_file
                    }
                )
            )
        }
    }

    val modeOptions = altimeterModeOptions(viewModel.hasBarometer)
    val mode = viewModel.mode
    val conditionalState = viewModel.conditionalState
    val altitude = viewModel.altitude?.let { formatService.formatDistance(it) } ?: stringResource(R.string.dash)
    val overrideSummary = formatService.formatDistance(viewModel.altitudeOverride)
    val forceCalibrationSummary = stringResource(
        R.string.fused_altimeter_force_calibration_summary,
        formatService.formatDuration(viewModel.forcedRecalibrationInterval)
    )
    val demSummary = viewModel.demVersion ?: stringResource(R.string.built_in_dem)
    var showForceCalibrationPicker by remember { mutableStateOf(false) }

    val forceCalibrationTitle = stringResource(R.string.fused_altimeter_force_calibration)
    val sourceTitle = stringResource(R.string.source)
    val overrideTitle = stringResource(R.string.pref_elevation_override_title)
    val calibrationTitle = stringResource(R.string.calibrate)
    val demTitle = stringResource(R.string.plugin_digital_elevation_model)
    val gpsTitle = stringResource(R.string.gps)
    val overrideFooter = stringResource(R.string.altimeter_override_explanation)
    val calibrationFooter = stringResource(R.string.altimeter_calibration_explanation)
    val altitudeIcon = rememberVectorPainter(TrailIcons.Altitude)

    TrailSensePage(
        title = stringResource(R.string.pref_altimeter_calibration_title),
        onBack = rememberGoBack(),
        backLabel = stringResource(R.string.back)
    ) {
        item("hero") {
            CalibrationHero(
                label = stringResource(R.string.elevation),
                value = altitude,
                icon = altitudeIcon,
                status = optionLabel(modeOptions, mode)
            )
        }

        trailSenseSection("source", title = sourceTitle) {
            row { position ->
                SettingsPickerRow(
                    label = sourceTitle,
                    selected = optionLabel(modeOptions, mode),
                    onClick = {
                        context.pickOption(sourceTitle, modeOptions, mode) {
                            viewModel.changeMode(it)
                        }
                    },
                    position = position
                )
            }
        }

        if (conditionalState.canProvideOverrides) {
            trailSenseSection("override", title = overrideTitle, footer = overrideFooter) {
                row { position ->
                    SettingsPickerRow(
                        label = overrideTitle,
                        selected = overrideSummary,
                        onClick = {
                            CustomUiUtils.pickElevation(
                                context,
                                viewModel.altitudeOverride,
                                context.getString(R.string.pref_elevation_override_title)
                            ) { elevation, _ ->
                                if (elevation != null) {
                                    viewModel.changeAltitudeOverride(elevation)
                                }
                            }
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.override_from_gps),
                        onClick = viewModel::setOverrideFromGPS,
                        position = position
                    )
                }
                if (conditionalState.hasBarometer) {
                    row { position ->
                        SettingsActionRow(
                            label = stringResource(R.string.elevation_from_pressure),
                            summary = stringResource(R.string.pref_elevation_override_sea_level_title),
                            onClick = {
                                Pickers.number(
                                    context,
                                    context.getString(R.string.pref_elevation_override_sea_level_dialog_title),
                                    context.getString(R.string.pref_elevation_override_sea_level_dialog_msg),
                                    viewModel.seaLevelPressureOverride,
                                    allowDecimals = true,
                                    allowNegative = false,
                                    hint = context.getString(R.string.pressure)
                                ) {
                                    if (it != null) {
                                        viewModel.setOverrideFromBarometer(it.toFloat())
                                    }
                                }
                            },
                            position = position
                        )
                    }
                }
            }
        }

        if (conditionalState.isFused) {
            trailSenseSection("calibration", title = calibrationTitle, footer = calibrationFooter) {
                row { position ->
                    SettingsPickerRow(
                        label = forceCalibrationTitle,
                        selected = forceCalibrationSummary,
                        onClick = { showForceCalibrationPicker = true },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.continuous_calibration),
                        summary = stringResource(R.string.continuous_calibration_description),
                        checked = viewModel.continuousCalibration,
                        onCheckedChange = viewModel::changeContinuousCalibration,
                        position = position
                    )
                }
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.reset_calibration),
                        onClick = viewModel::resetCalibration,
                        position = position
                    )
                }
            }
        }

        trailSenseSection("gps", title = gpsTitle) {
            if (conditionalState.showSamples) {
                row { position ->
                    SettingsPickerRow(
                        label = stringResource(R.string.samples),
                        selected = viewModel.samples.toString(),
                        onClick = {
                            val samples = (1..8).toList()
                            Pickers.item(
                                context,
                                context.getString(R.string.samples),
                                samples.map { it.toString() },
                                samples.indexOf(viewModel.samples).coerceAtLeast(0)
                            ) {
                                if (it != null) {
                                    viewModel.changeSamples(samples[it])
                                }
                            }
                        },
                        position = position
                    )
                }
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_nmea_elevation_title),
                    summary = stringResource(R.string.pref_nmea_elevation_summary),
                    checked = viewModel.useNmea,
                    onCheckedChange = viewModel::changeUseNmea,
                    position = position
                )
            }
        }

        trailSenseSection("dem", title = demTitle) {
            row { position ->
                SettingsActionRow(
                    label = demTitle,
                    summary = demSummary,
                    onClick = { scope.launch { loadDEM(context, viewModel) } },
                    position = position
                )
            }
            if (conditionalState.clearDemVisible) {
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.remove_dem),
                        onClick = { scope.launch { clearDEM(context, viewModel) } },
                        position = position
                    )
                }
            }
        }
    }

    if (showForceCalibrationPicker) {
        DurationPickerDialog(
            title = forceCalibrationTitle,
            duration = viewModel.forcedRecalibrationInterval,
            onDurationPicked = {
                if (it != null) {
                    viewModel.changeForcedRecalibrationInterval(it)
                }
                showForceCalibrationPicker = false
            },
            onDismiss = { showForceCalibrationPicker = false }
        )
    }
}

/** Asks for a DEM zip, loads it with a progress dialog and tells the page the model changed. */
private suspend fun loadDEM(context: Context, viewModel: CalibrateAltimeterViewModel) {
    val instructions = context.getString(
        R.string.digital_elevation_model_link,
        URLs.DEM
    ).toSpannable()
    LinkifyCompat.addLinks(instructions, WEB_URLS)
    val cancelled = CoroutineAlerts.dialog(
        context,
        context.getString(R.string.plugin_digital_elevation_model),
        instructions,
        allowLinks = true
    )

    if (cancelled) {
        return
    }

    // The file picker returns its result through the activity, the one Activity-only API this page needs.
    val retriever = context as IntentResultRetriever
    val source = IntentUriPicker(retriever, context).open(listOf("application/zip")).getOrNull() ?: return

    val loader = DigitalElevationModelLoader()
    try {
        Alerts.withProgress(context, context.getString(R.string.loading)) { setProgress ->
            loader.load(source).collect(setProgress)
            onMain {
                viewModel.onDemLoaded()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onMain {
            viewModel.onDemLoadFailed()
        }
    }
}

private suspend fun clearDEM(context: Context, viewModel: CalibrateAltimeterViewModel) {
    val cancelled = CoroutineAlerts.dialog(context, context.getString(R.string.remove_dem))
    if (cancelled) {
        return
    }
    val loader = DigitalElevationModelLoader()
    Alerts.withLoading(context, context.getString(R.string.loading)) {
        loader.clear()
        onMain {
            viewModel.onDemCleared()
        }
    }
}

@Composable
private fun altimeterModeOptions(hasBarometer: Boolean): List<SettingsOption> {
    val gpsBarometer = stringResource(R.string.altimeter_mode_gps_barometer)
    val gps = stringResource(R.string.gps)
    val barometer = stringResource(R.string.barometer)
    val demBarometer = stringResource(R.string.altimeter_mode_dem_barometer)
    val dem = stringResource(R.string.digital_elevation_model_abbreviation)
    val manual = stringResource(R.string.manual)
    return remember(hasBarometer, gpsBarometer, gps, barometer, demBarometer, dem, manual) {
        listOfNotNull(
            if (hasBarometer) SettingsOption("gps_barometer", gpsBarometer) else null,
            SettingsOption("gps", gps),
            if (hasBarometer) SettingsOption("barometer", barometer) else null,
            if (hasBarometer) SettingsOption("dem_barometer", demBarometer) else null,
            SettingsOption("dem", dem),
            SettingsOption("override", manual)
        )
    }
}

/** The preference keys the altimeter page reads and writes directly. */
internal data class AltimeterPreferenceKeys(
    val mode: String,
    val seaLevelPressureOverride: String,
    val continuousCalibration: String,
    val nmea: String
)

internal enum class AltimeterMessage { Done, OverrideUpdated, Loaded, InvalidDem }

internal data class AltimeterConditionalState(
    val hasBarometer: Boolean,
    val isFused: Boolean,
    val canProvideOverrides: Boolean,
    val showSamples: Boolean,
    val clearDemVisible: Boolean
)

/**
 * The altimeter calibration page's state: the live elevation, the chosen source and its overrides, and the DEM in
 * use. Plain Kotlin over the preferences and sensors; passing messages go out through [messages].
 */
internal class CalibrateAltimeterViewModel(
    private val prefs: UserPreferences,
    private val cache: IPreferences,
    private val sensorService: SensorService,
    private val gps: IGPS,
    private val keys: AltimeterPreferenceKeys
) : ViewModel() {

    private val barometer: IBarometer = sensorService.getBarometer()
    private var altimeter: IAltimeter
    private val throttle = Throttle(20)
    private var altimeterStarted = false
    private var isShown = false
    private val distanceUnits: DistanceUnits = prefs.baseDistanceUnits
    private var lastMode: UserPreferences.AltimeterMode
    private val updateTimer = CoroutineTimer { updateAltitude() }
    private val overridePopulationRunner = CoroutineQueueRunner(dispatcher = Dispatchers.Default)
    private val onAltimeterReading: () -> Boolean = ::updateAltitude
    private val onPreferenceChanged: (String) -> Boolean = { key ->
        if (key == keys.seaLevelPressureOverride) {
            viewModelScope.launch { restartAltimeter() }
        }
        true
    }

    private val _messages = Channel<AltimeterMessage>(Channel.BUFFERED)
    val messages: Flow<AltimeterMessage> = _messages.receiveAsFlow()

    val hasBarometer: Boolean
        get() = prefs.weather.hasBarometer

    /** The live elevation, or null before the first reading. */
    var altitude by mutableStateOf<Distance?>(null)
        private set
    var mode by mutableStateOf("")
        private set
    var altitudeOverride by mutableStateOf(Distance.meters(0f))
        private set
    var samples by mutableStateOf(prefs.altimeterSamples)
        private set
    var continuousCalibration by mutableStateOf(true)
        private set
    var useNmea by mutableStateOf(false)
        private set
    var forcedRecalibrationInterval by mutableStateOf(prefs.altimeter.fusedAltimeterForcedRecalibrationInterval)
        private set
    var conditionalState by mutableStateOf(loadConditionalState())
        private set
    /** The external DEM's version, or null for the built-in one. */
    var demVersion by mutableStateOf<String?>(null)
        private set

    val seaLevelPressureOverride: Float
        get() = prefs.seaLevelPressureOverride

    init {
        ensureDefaultMode()
        altimeter = sensorService.getAltimeter()
        lastMode = prefs.altimeterMode
        mode = getModePreference()
        refreshOverrideSummary()
        cache.onChange.subscribe(onPreferenceChanged)
        refreshConditionalState()
    }

    /** Runs the altimeter while the page is shown. */
    fun start() {
        isShown = true
        startAltimeter()
        updateTimer.interval(200)
    }

    fun stop() {
        isShown = false
        stopAltimeter()
        updateTimer.stop()
        overridePopulationRunner.cancel()
    }

    override fun onCleared() {
        stop()
        cache.onChange.unsubscribe(onPreferenceChanged)
    }

    fun changeMode(value: String) {
        cache.putString(keys.mode, value)
        mode = value
        onAltimeterModeChanged()
    }

    fun changeContinuousCalibration(enabled: Boolean) {
        continuousCalibration = enabled
        cache.putBoolean(keys.continuousCalibration, enabled)
        restartAltimeter()
    }

    fun changeUseNmea(enabled: Boolean) {
        useNmea = enabled
        cache.putBoolean(keys.nmea, enabled)
        restartAltimeter()
    }

    fun changeSamples(value: Int) {
        prefs.altimeterSamples = value
        samples = value
        restartAltimeter()
    }

    fun changeForcedRecalibrationInterval(interval: Duration) {
        prefs.altimeter.fusedAltimeterForcedRecalibrationInterval = interval
        forcedRecalibrationInterval = interval
    }

    fun resetCalibration() {
        FusedAltimeter.clearCachedCalibration(cache)
        _messages.trySend(AltimeterMessage.Done)
    }

    fun onDemLoaded() {
        onAltimeterModeChanged()
        _messages.trySend(AltimeterMessage.Loaded)
    }

    fun onDemLoadFailed() {
        _messages.trySend(AltimeterMessage.InvalidDem)
    }

    fun onDemCleared() {
        onAltimeterModeChanged()
        _messages.trySend(AltimeterMessage.Done)
    }

    private fun startAltimeter() {
        if (altimeterStarted) {
            return
        }
        altimeterStarted = true
        altimeter.start(onAltimeterReading)
    }

    private fun stopAltimeter() {
        altimeterStarted = false
        altimeter.stop(onAltimeterReading)
    }

    private fun restartAltimeter() {
        stopAltimeter()
        altimeter = sensorService.getAltimeter()
        if (isShown) {
            startAltimeter()
        }
    }

    fun setOverrideFromBarometer(seaLevelPressure: Float) {
        viewModelScope.launch {
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
                        _messages.trySend(AltimeterMessage.OverrideUpdated)
                    }
                }
            }
        }
    }

    fun setOverrideFromGPS() {
        viewModelScope.launch {
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
                        _messages.trySend(AltimeterMessage.OverrideUpdated)
                    }
                }
            }
        }
    }

    private fun updateAltitudeOverride() {
        viewModelScope.launch {
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
        mode = getModePreference()
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

        altitude = Distance.meters(altimeter.altitude).convertTo(distanceUnits)

        if (lastMode != prefs.altimeterMode) {
            onAltimeterModeChanged()
        }

        refreshOverrideSummary()

        return true
    }

    fun changeAltitudeOverride(elevation: Distance) {
        viewModelScope.launch {
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

    private fun refreshConditionalState() {
        conditionalState = loadConditionalState()
        forcedRecalibrationInterval = prefs.altimeter.fusedAltimeterForcedRecalibrationInterval
        continuousCalibration = cache.getBoolean(keys.continuousCalibration) ?: true
        useNmea = cache.getBoolean(keys.nmea) ?: false
        updateDemSummary()
    }

    private fun updateDemSummary() {
        viewModelScope.launch {
            val version = DEMRepo.getInstance().getVersion()
            onMain {
                demVersion = version
                conditionalState = loadConditionalState()
            }
        }
    }

    private fun refreshOverrideSummary() {
        altitudeOverride = Distance.meters(prefs.altitudeOverride).convertTo(distanceUnits)
    }

    private fun loadConditionalState(): AltimeterConditionalState {
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

    private fun ensureDefaultMode() {
        if (cache.getString(keys.mode) == null) {
            cache.putString(keys.mode, if (prefs.weather.hasBarometer) "gps_barometer" else "gps")
        }
    }

    private fun getModePreference(): String {
        return cache.getString(keys.mode)
            ?: if (prefs.weather.hasBarometer) "gps_barometer" else "gps"
    }
}
