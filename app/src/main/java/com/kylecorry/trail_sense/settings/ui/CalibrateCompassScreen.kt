@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.shared.icons.trail.*
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.trail_sense.shared.extensions.asLiveData
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.andromeda.sense.compass.ICompass
import com.kylecorry.andromeda.sense.location.IGPS
import com.kylecorry.luna.time.Throttle
import com.kylecorry.sol.science.geophysics.Geophysics
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.declination.DeclinationFactory
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.sensors.compass.CompassSource
import com.kylecorry.trail_sense.shared.sensors.providers.CompassProvider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlin.math.roundToInt
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDialog

@Composable
fun CalibrateCompassToolScreen() {
    val context = LocalContext.current.applicationContext
    val cache = remember(context) { PreferencesSubsystem.getInstance(context).preferences }
    val viewModel = viewModel {
        CalibrateCompassViewModel(
            prefs = UserPreferences(context),
            formatService = FormatService.getInstance(context),
            sensorService = SensorService(context)
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val effects = LocalScreenEffects.current

    // The compass restarts when its north, smoothing or source changes while the page is shown.
    DisposableEffect(viewModel, lifecycleOwner, cache) {
        val resetPrefs = listOf(
            context.getString(R.string.pref_use_true_north),
            context.getString(R.string.pref_compass_filter_amt),
            context.getString(R.string.pref_compass_source)
        )
        val changes = cache.onChange.asLiveData()
        val observer = Observer<String> {
            if (resetPrefs.contains(it)) {
                viewModel.resetCompass()
            }
        }
        changes.observe(lifecycleOwner, observer)
        onDispose { changes.removeObserver(observer) }
    }

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { viewModel.onPause() }
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            when (message) {
                CompassMessage.DeclinationOverrideUpdated -> effects.showMessage(
                    context.getString(R.string.declination_override_updated_toast)
                )

                is CompassMessage.AccuracyImproved -> effects.showMessage(
                    context.getString(R.string.compass_accuracy_improved, message.accuracy)
                )
            }
        }
    }

    CalibrateCompassContent(viewModel, cache)
}

/** A passing message the compass page shows. */
internal sealed interface CompassMessage {
    data object DeclinationOverrideUpdated : CompassMessage
    data class AccuracyImproved(val accuracy: String) : CompassMessage
}

/**
 * The compass page's live readings: the compass and GPS run while the page is resumed, and the readings, the
 * declination override and passing messages come out as plain values.
 */
internal class CalibrateCompassViewModel(
    val prefs: UserPreferences,
    private val formatService: FormatService,
    private val sensorService: SensorService
) : ViewModel() {

    private val throttle = Throttle(20)
    private val gps: IGPS by lazy { sensorService.getGPS() }
    val hasCompass by lazy { sensorService.hasCompass() }

    private var compass: ICompass = sensorService.getCompass()
    private var prevQuality = Quality.Unknown

    var readings by mutableStateOf<CompassReadings?>(null)
        private set
    var declinationOverride by mutableStateOf(prefs.declinationOverride)
        private set

    private val _messages = Channel<CompassMessage>(Channel.BUFFERED)
    val messages: Flow<CompassMessage> = _messages.receiveAsFlow()

    fun onResume() {
        startCompass()
        if (!gps.hasValidReading) {
            gps.start(::onLocationUpdate)
        }
    }

    fun onPause() {
        stopCompass()
        gps.stop(::onLocationUpdate)
        gps.stop(::onUpdateDeclinationFromGpsCallback)
    }

    override fun onCleared() {
        onPause()
    }

    fun resetCompass() {
        stopCompass()
        compass = sensorService.getCompass()
        startCompass()
    }

    fun updateDeclinationFromGps() {
        if (gps.hasValidReading) {
            onUpdateDeclinationFromGpsCallback()
        } else {
            gps.start(::onUpdateDeclinationFromGpsCallback)
        }
    }

    fun changeDeclinationOverride(declination: Float) {
        prefs.declinationOverride = declination
        declinationOverride = declination
        update()
    }

    private fun onLocationUpdate(): Boolean {
        update()
        return false
    }

    private fun getDeclination(): Float {
        return DeclinationFactory().getDeclinationStrategy(prefs, gps).getDeclination()
    }

    private fun onUpdateDeclinationFromGpsCallback(): Boolean {
        val declination = Geophysics.getGeomagneticDeclination(gps.location, gps.altitude)
        prefs.declinationOverride = declination
        declinationOverride = declination
        update()
        _messages.trySend(CompassMessage.DeclinationOverrideUpdated)
        return false
    }

    private fun startCompass() {
        compass.start(::onCompassUpdate)
    }

    private fun stopCompass() {
        compass.stop(::onCompassUpdate)
    }

    private fun onCompassUpdate(): Boolean {
        update()
        return true
    }

    fun update() {
        if (throttle.isThrottled()) {
            return
        }

        if (prevQuality != Quality.Unknown && prevQuality != compass.quality) {
            if (compass.quality.ordinal > prevQuality.ordinal) {
                _messages.trySend(CompassMessage.AccuracyImproved(getCompassAccuracy()))
            }
            prevQuality = compass.quality
        }

        compass.declination = getDeclination()

        readings = CompassReadings(
            azimuth = formatService.formatDegrees(compass.rawBearing, replace360 = true),
            declination = formatService.formatDegrees(compass.declination),
            accuracy = getCompassAccuracy(),
            quality = compass.quality
        )
        declinationOverride = prefs.declinationOverride
    }

    private fun getCompassAccuracy(): String {
        return formatService.formatQuality(compass.quality)
    }
}

internal data class CompassReadings(
    val azimuth: String,
    val declination: String,
    val accuracy: String,
    val quality: Quality
)

@Composable
private fun CalibrateCompassContent(viewModel: CalibrateCompassViewModel, cache: IPreferences) {
    val context = LocalContext.current
    val prefs = viewModel.prefs
    val hasCompass = viewModel.hasCompass
    val compassSourceOptions = remember(context) { compassSourceOptions(context) }
    val dash = stringResource(R.string.dash)
    val sourceTitle = stringResource(R.string.source)
    val declinationOverrideTitle = stringResource(R.string.compass_declination_override)
    val declinationHint = stringResource(R.string.declination)
    val goBack = rememberGoBack()

    val readings = viewModel.readings
    val azimuth = readings?.azimuth ?: dash
    val accuracy = readings?.accuracy
    val quality = readings?.quality ?: Quality.Unknown
    val declination = readings?.declination ?: dash
    val declinationOverride = viewModel.declinationOverride
    var source by remember { mutableStateOf(prefs.compass.source.id) }
    var smoothing by remember { mutableStateOf(prefs.compass.compassSmoothing) }
    var useTrueNorth by remember {
        mutableStateOf(context.getBoolean(cache, R.string.pref_use_true_north, true))
    }
    var useAutoDeclination by remember { mutableStateOf(prefs.useAutoDeclination) }
    var showCalibrationDialog by remember { mutableStateOf(false) }

    val sensorTitle = stringResource(R.string.sensor)
    val declinationTitle = stringResource(R.string.compass_declination)
    val declinationFooter = stringResource(R.string.compass_declination_explanation)
    val calibrateFooter = stringResource(R.string.compass_calibration_explanation)
    val compassIcon = rememberVectorPainter(TrailIcons.CompassIcon)

    TrailSensePage(
        title = stringResource(R.string.pref_compass_sensor_title),
        onBack = goBack,
        backLabel = stringResource(R.string.back)
    ) {
        if (hasCompass) {
            item("hero") {
                CalibrationHero(
                    label = stringResource(R.string.compass_azimuth),
                    value = azimuth,
                    icon = compassIcon,
                    status = accuracy,
                    tone = quality.toCalibrationTone(),
                    supportingText = calibrateFooter
                ) {
                    com.kylecorry.trail_sense.shared.views.compose.TrailSenseButton(
                        text = stringResource(R.string.calibrate),
                        onClick = { showCalibrationDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            trailSenseSection("sensor", title = sensorTitle) {
                row { position ->
                    SettingsPickerRow(
                        label = sourceTitle,
                        selected = optionLabel(compassSourceOptions, source),
                        onClick = {
                            context.pickOption(sourceTitle, compassSourceOptions, source) {
                                source = it
                                context.putString(cache, R.string.pref_compass_source, it)
                                viewModel.resetCompass()
                            }
                        },
                        position = position
                    )
                }
                row { position ->
                    CalibrationSliderRow(
                        title = stringResource(R.string.smoothing),
                        summary = stringResource(R.string.smoothing_summary),
                        value = smoothing,
                        range = 1..100,
                        onValueChange = {
                            smoothing = it
                            cache.putInt(context.getString(R.string.pref_compass_filter_amt), it)
                            viewModel.resetCompass()
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.true_north),
                        checked = useTrueNorth,
                        onCheckedChange = {
                            useTrueNorth = it
                            prefs.compass.useTrueNorth = it
                            viewModel.resetCompass()
                        },
                        position = position
                    )
                }
            }
        }

        trailSenseSection("declination", title = declinationTitle, footer = declinationFooter) {
            row { position ->
                SettingsInfoRow(
                    label = declinationTitle,
                    summary = declination,
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.compass_auto_declination),
                    summary = stringResource(R.string.pref_auto_declination_summary),
                    checked = useAutoDeclination,
                    onCheckedChange = {
                        useAutoDeclination = it
                        prefs.useAutoDeclination = it
                        viewModel.update()
                    },
                    position = position
                )
            }
            if (!useAutoDeclination) {
                row { position ->
                    SettingsPickerRow(
                        label = declinationOverrideTitle,
                        selected = stringResource(R.string.degree_format, declinationOverride),
                        onClick = {
                            Pickers.number(
                                context,
                                declinationOverrideTitle,
                                null,
                                declinationOverride,
                                allowDecimals = true,
                                allowNegative = true,
                                hint = declinationHint
                            ) {
                                if (it != null) {
                                    viewModel.changeDeclinationOverride(it.toFloat())
                                }
                            }
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.override_from_gps),
                        onClick = viewModel::updateDeclinationFromGps,
                        position = position
                    )
                }
            }
        }
    }

    if (showCalibrationDialog) {
        TrailSenseDialog(
            onDismissRequest = {},
            title = stringResource(R.string.calibrate_compass_dialog_title),
            confirmText = stringResource(android.R.string.ok),
            onConfirm = { showCalibrationDialog = false }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)) {
                Text(
                    text = stringResource(
                        R.string.calibrate_compass_dialog_content,
                        stringResource(android.R.string.ok)
                    )
                )
                CompassCalibrationContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(TrailSenseShape.groupOuter))
                )
                val currentAccuracy = accuracy
                if (currentAccuracy != null) {
                    Text(
                        text = stringResource(R.string.compass_reported_accuracy, currentAccuracy),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** The compass sources this device has, named for the picker. */
private fun compassSourceOptions(context: Context): List<SettingsOption> {
    val availableSources = CompassProvider.getAvailableSources(context)
    return mapOf(
        CompassSource.RotationVector to context.getString(R.string.compass_source_mag_gyro),
        CompassSource.GeomagneticRotationVector to context.getString(R.string.magnetometer),
        CompassSource.CustomRotationVector to context.getString(R.string.magnetometer_gyro_custom),
        CompassSource.CustomMagnetometer to context.getString(R.string.compass_source_legacy_ts),
        CompassSource.Orientation to context.getString(R.string.compass_source_legacy_android)
    ).filter { availableSources.contains(it.key) }
        .map { SettingsOption(it.key.id, it.value) }
}
