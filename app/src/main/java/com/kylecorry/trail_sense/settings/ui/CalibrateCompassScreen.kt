@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.dialogs.Alerts
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

@Composable
fun CalibrateCompassToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { CalibrateCompassController(host) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.observePreferences(lifecycleOwner)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_PAUSE -> controller.onPause()
                Lifecycle.Event.ON_DESTROY -> controller.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.onPause()
            controller.onDestroy()
        }
    }

    CalibrateCompassContent(controller)
}

private class CalibrateCompassController(private val host: TrailSenseDestinationHost) {

    val prefs by lazy { UserPreferences(host.requireContext()) }
    private val cache by lazy { PreferencesSubsystem.getInstance(host.requireContext()).preferences }
    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }
    private val sensorService by lazy { SensorService(host.requireContext()) }
    private val throttle = Throttle(20)
    private val gps: IGPS by lazy { sensorService.getGPS() }
    val hasCompass by lazy { sensorService.hasCompass() }

    private var compass: ICompass = sensorService.getCompass()
    private var prevQuality = Quality.Unknown

    private var setCompassReadings: ((CompassReadings) -> Unit)? = null
    private var setDeclinationOverride: ((Float) -> Unit)? = null

    fun observePreferences(lifecycleOwner: LifecycleOwner) {
        val resetPrefs = listOf(
            host.getString(R.string.pref_use_true_north),
            host.getString(R.string.pref_compass_filter_amt),
            host.getString(R.string.pref_compass_source)
        )
        cache.onChange.asLiveData().observe(lifecycleOwner) {
            if (resetPrefs.contains(it)) {
                resetCompass()
            }
        }
    }

    fun setCompassReadings(updater: (CompassReadings) -> Unit) {
        setCompassReadings = updater
    }

    fun setDeclinationOverride(updater: (Float) -> Unit) {
        setDeclinationOverride = updater
    }

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

    fun onDestroy() {
        setCompassReadings = null
        setDeclinationOverride = null
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

    fun pickOption(
        title: String,
        options: List<CompassOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        Pickers.item(
            host.requireContext(),
            title,
            options.map { it.label },
            options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        ) { index ->
            if (index != null) {
                onSelected(options[index].value)
            }
        }
    }

    fun pickDeclinationOverride(current: Float, onPicked: (Float) -> Unit) {
        Pickers.number(
            host.requireContext(),
            host.getString(R.string.compass_declination_override),
            null,
            current,
            allowDecimals = true,
            allowNegative = true,
            hint = host.getString(R.string.declination)
        ) {
            if (it != null) {
                val declination = it.toFloat()
                prefs.declinationOverride = declination
                onPicked(declination)
                update()
            }
        }
    }

    fun getCompassSourceOptions(): List<CompassOption> {
        val availableSources = CompassProvider.getAvailableSources(host.requireContext())
        return mapOf(
            CompassSource.RotationVector to host.getString(R.string.compass_source_mag_gyro),
            CompassSource.GeomagneticRotationVector to host.getString(R.string.magnetometer),
            CompassSource.CustomRotationVector to host.getString(R.string.magnetometer_gyro_custom),
            CompassSource.CustomMagnetometer to host.getString(R.string.compass_source_legacy_ts),
            CompassSource.Orientation to host.getString(R.string.compass_source_legacy_android)
        ).filter { availableSources.contains(it.key) }
            .map { CompassOption(it.key.id, it.value) }
    }

    fun optionLabel(options: List<CompassOption>, selected: String): String {
        return options.firstOrNull { it.value == selected }?.label ?: selected
    }

    fun getBoolean(key: Int, defaultValue: Boolean = false): Boolean {
        return cache.getBoolean(host.getString(key)) ?: defaultValue
    }

    fun putString(key: Int, value: String) {
        cache.putString(host.getString(key), value)
    }

    fun putInt(key: Int, value: Int) {
        cache.putInt(host.getString(key), value)
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
        setDeclinationOverride?.invoke(declination)
        update()
        Alerts.toast(
            host.requireContext(),
            host.getString(R.string.declination_override_updated_toast)
        )
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
                Alerts.toast(
                    host.requireContext(),
                    host.getString(R.string.compass_accuracy_improved, getCompassAccuracy())
                )
            }
            prevQuality = compass.quality
        }

        compass.declination = getDeclination()

        setCompassReadings?.invoke(
            CompassReadings(
                azimuth = formatService.formatDegrees(compass.rawBearing, replace360 = true),
                declination = formatService.formatDegrees(compass.declination),
                accuracy = getCompassAccuracy(),
                declinationOverride = prefs.declinationOverride
            )
        )
    }

    private fun getCompassAccuracy(): String {
        return formatService.formatQuality(compass.quality)
    }
}

private data class CompassReadings(
    val azimuth: String,
    val declination: String,
    val accuracy: String,
    val declinationOverride: Float
)

private data class CompassOption(
    val value: String,
    val label: String
)

@Composable
private fun CalibrateCompassContent(controller: CalibrateCompassController) {
        val prefs = controller.prefs
        val hasCompass = controller.hasCompass
        val compassSourceOptions = remember { controller.getCompassSourceOptions() }
        val dash = stringResource(R.string.dash)
        val sourceTitle = stringResource(R.string.source)

        var azimuth by remember { mutableStateOf(dash) }
        var accuracy by remember { mutableStateOf(dash) }
        var declination by remember { mutableStateOf(dash) }
        var source by remember { mutableStateOf(prefs.compass.source.id) }
        var smoothing by remember { mutableStateOf(prefs.compass.compassSmoothing) }
        var useTrueNorth by remember {
            mutableStateOf(controller.getBoolean(R.string.pref_use_true_north, true))
        }
        var useAutoDeclination by remember {
            mutableStateOf(prefs.useAutoDeclination)
        }
        var declinationOverride by remember {
            mutableStateOf(prefs.declinationOverride)
        }
        var showCalibrationDialog by remember { mutableStateOf(false) }

        controller.setCompassReadings {
            azimuth = it.azimuth
            accuracy = it.accuracy
            declination = it.declination
            declinationOverride = it.declinationOverride
        }
        controller.setDeclinationOverride {
            declinationOverride = it
        }

        val qualityGood = stringResource(R.string.quality_good)
        val qualityPoor = stringResource(R.string.quality_poor)

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = TrailSenseSize.maximumContentWidth)
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(
                    start = TrailSenseSpacing.screen,
                    end = TrailSenseSpacing.screen,
                    top = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.xxxl
                ),
                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
            ) {
                item("header") {
                    Text(
                        text = stringResource(R.string.pref_compass_sensor_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(
                            top = TrailSenseSpacing.sm,
                            bottom = TrailSenseSpacing.xs
                        )
                    )
                }

                if (hasCompass) {
                    item("hero") {
                        CompassHeroCard(
                            azimuth = azimuth,
                            accuracy = accuracy,
                            qualityGood = qualityGood,
                            qualityPoor = qualityPoor,
                            onCalibrate = { showCalibrationDialog = true }
                        )
                    }

                    item("status") {
                        CalibrateSettingsSection(
                            title = stringResource(R.string.sensor)
                        ) {
                            SettingsInfoRow(
                                label = stringResource(R.string.compass_azimuth),
                                summary = azimuth
                            )
                            SettingsPickerRow(
                                label = sourceTitle,
                                selected = controller.optionLabel(compassSourceOptions, source),
                                onClick = {
                                    controller.pickOption(
                                        sourceTitle,
                                        compassSourceOptions,
                                        source
                                    ) {
                                        source = it
                                        controller.putString(R.string.pref_compass_source, it)
                                        controller.resetCompass()
                                    }
                                }
                            )
                            CompassSmoothingRow(
                                smoothing = smoothing,
                                onSmoothingChange = {
                                    smoothing = it
                                    controller.putInt(R.string.pref_compass_filter_amt, it)
                                    controller.resetCompass()
                                }
                            )
                            TrailSenseDivider()
                            SettingsSwitchRow(
                                label = stringResource(R.string.true_north),
                                checked = useTrueNorth,
                                onCheckedChange = {
                                    useTrueNorth = it
                                    prefs.compass.useTrueNorth = it
                                    controller.resetCompass()
                                }
                            )
                        }
                    }
                }

                item("declination_section") {
                    CalibrateSettingsSection(
                        title = stringResource(R.string.compass_declination)
                    ) {
                        SettingsInfoRow(
                            label = stringResource(R.string.compass_declination),
                            summary = declination
                        )
                        SettingsSwitchRow(
                            label = stringResource(R.string.compass_auto_declination),
                            summary = stringResource(R.string.pref_auto_declination_summary),
                            checked = useAutoDeclination,
                            onCheckedChange = {
                                useAutoDeclination = it
                                prefs.useAutoDeclination = it
                                controller.update()
                            }
                        )
                        TrailSenseDivider()
                        SettingsPickerRow(
                            label = stringResource(R.string.compass_declination_override),
                            selected = stringResource(
                                R.string.degree_format,
                                declinationOverride
                            ),
                            enabled = !useAutoDeclination,
                            onClick = {
                                controller.pickDeclinationOverride(declinationOverride) {
                                    declinationOverride = it
                                }
                            }
                        )
                        SettingsActionRow(
                            label = stringResource(R.string.override_from_gps),
                            enabled = !useAutoDeclination,
                            onClick = controller::updateDeclinationFromGps
                        )
                    }
                }
            }
            }
        }

        if (showCalibrationDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(text = stringResource(R.string.calibrate_compass_dialog_title))
                },
                text = {
                    Column {
                        Text(
                            text = stringResource(
                                R.string.calibrate_compass_dialog_content,
                                stringResource(android.R.string.ok)
                            )
                        )
                        if (hasCompass) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = TrailSenseSpacing.lg),
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                CompassCalibrationContent(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCalibrationDialog = false }) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                }
            )
        }
}

@Composable
private fun CompassSmoothingRow(
    smoothing: Int,
    onSmoothingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TrailSenseShape.groupInner),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
    Column(
        modifier = Modifier.padding(
            horizontal = TrailSenseSpacing.lg,
            vertical = TrailSenseSpacing.md
        )
    ) {
        Text(
            text = stringResource(R.string.smoothing),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${stringResource(R.string.smoothing_summary)} ($smoothing)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = smoothing.toFloat(),
            onValueChange = {
                onSmoothingChange(it.roundToInt().coerceIn(1, 100))
            },
            valueRange = 1f..100f,
            steps = 98
        )
    }
    }
}

@Composable
private fun CompassHeroCard(
    azimuth: String,
    accuracy: String,
    qualityGood: String,
    qualityPoor: String,
    onCalibrate: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Color-code the calibration quality state without changing any behavior.
    val targetQualityColor = when (accuracy) {
        qualityGood -> MaterialTheme.colorScheme.tertiary
        qualityPoor -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }
    val qualityColor by animateColorAsState(
        targetValue = targetQualityColor,
        animationSpec = tween(durationMillis = 250),
        label = "quality_color"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(TrailSenseSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.ic_compass_icon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(TrailSenseSpacing.md))
                Text(
                    text = stringResource(R.string.compass_azimuth),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = azimuth,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Color-coded calibration quality state
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = qualityColor,
                        modifier = Modifier.fillMaxSize()
                    ) {}
                }
                Spacer(Modifier.width(TrailSenseSpacing.sm))
                Text(
                    text = stringResource(R.string.compass_reported_accuracy, accuracy),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = qualityColor
                )
            }

            TextButton(
                onClick = onCalibrate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.calibrate),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CalibrateSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(
                start = TrailSenseSpacing.lg,
                bottom = TrailSenseSpacing.sm
            )
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = Color.Transparent
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)) {
                content()
            }
        }
    }
}

@Composable
private fun TrailSenseDivider() {
    Spacer(modifier = Modifier.height(TrailSenseSpacing.xxs))
}
