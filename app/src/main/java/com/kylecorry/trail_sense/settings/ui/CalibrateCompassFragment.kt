package com.kylecorry.trail_sense.settings.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.alerts.dialog
import com.kylecorry.andromeda.core.sensors.Quality
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.fragments.asLiveData
import com.kylecorry.andromeda.pickers.Pickers
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
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlin.math.roundToInt

class CalibrateCompassFragment : TrailSenseComposeFragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }
    private val cache by lazy { PreferencesSubsystem.getInstance(requireContext()).preferences }
    private val formatService by lazy { FormatService.getInstance(requireContext()) }
    private val sensorService by lazy { SensorService(requireContext()) }
    private val throttle = Throttle(20)
    private val gps: IGPS by lazy { sensorService.getGPS() }
    private val hasCompass by lazy { sensorService.hasCompass() }

    private lateinit var compass: ICompass
    private var prevQuality = Quality.Unknown

    private var setCompassReadings: ((CompassReadings) -> Unit)? = null
    private var setDeclinationOverride: ((Float) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        compass = sensorService.getCompass()
    }

    @Composable
    override fun FragmentContent() {
        val compassSourceOptions = remember { getCompassSourceOptions() }

        var azimuth by remember { mutableStateOf(getString(R.string.dash)) }
        var accuracy by remember { mutableStateOf(getString(R.string.dash)) }
        var declination by remember { mutableStateOf(getString(R.string.dash)) }
        var source by remember { mutableStateOf(prefs.compass.source.id) }
        var smoothing by remember { mutableStateOf(prefs.compass.compassSmoothing) }
        var useTrueNorth by remember {
            mutableStateOf(getBoolean(R.string.pref_use_true_north, true))
        }
        var useAutoDeclination by remember {
            mutableStateOf(prefs.useAutoDeclination)
        }
        var declinationOverride by remember {
            mutableStateOf(prefs.declinationOverride)
        }

        setCompassReadings = {
            azimuth = it.azimuth
            accuracy = it.accuracy
            declination = it.declination
            declinationOverride = it.declinationOverride
        }
        setDeclinationOverride = {
            declinationOverride = it
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = TrailSenseSpacing.lg,
                    end = TrailSenseSpacing.lg,
                    top = TrailSenseSpacing.lg,
                    bottom = TrailSenseSpacing.xxxl
                )
            ) {
                item("header") {
                    TrailSenseSectionHeader(
                        title = stringResource(R.string.pref_compass_sensor_title)
                    )
                }
                if (hasCompass) {
                    item("azimuth") {
                        SettingsInfoRow(
                            label = stringResource(R.string.compass_azimuth),
                            summary = azimuth,
                            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                        )
                    }
                    item("calibrate") {
                        SettingsActionRow(
                            label = stringResource(R.string.calibrate),
                            summary = getString(
                                R.string.compass_reported_accuracy,
                                accuracy
                            ),
                            onClick = ::showCalibrationDialog,
                            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                        )
                    }
                    item("source") {
                        SettingsPickerRow(
                            label = stringResource(R.string.source),
                            selected = optionLabel(compassSourceOptions, source),
                            onClick = {
                                pickOption(
                                    getString(R.string.source),
                                    compassSourceOptions,
                                    source
                                ) {
                                    source = it
                                    putString(R.string.pref_compass_source, it)
                                    resetCompass()
                                }
                            },
                            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                        )
                    }
                    item("smoothing") {
                        CompassSmoothingRow(
                            smoothing = smoothing,
                            onSmoothingChange = {
                                smoothing = it
                                putInt(R.string.pref_compass_filter_amt, it)
                                resetCompass()
                            },
                            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                        )
                        TrailSenseDivider()
                    }
                    item("true_north") {
                        SettingsSwitchRow(
                            label = stringResource(R.string.true_north),
                            checked = useTrueNorth,
                            onCheckedChange = {
                                useTrueNorth = it
                                prefs.compass.useTrueNorth = it
                                resetCompass()
                            },
                            modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                        )
                        TrailSenseDivider()
                    }
                }
                item("declination") {
                    SettingsInfoRow(
                        label = stringResource(R.string.compass_declination),
                        summary = declination,
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }
                item("auto_declination") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.compass_auto_declination),
                        summary = stringResource(R.string.pref_auto_declination_summary),
                        checked = useAutoDeclination,
                        onCheckedChange = {
                            useAutoDeclination = it
                            prefs.useAutoDeclination = it
                            update()
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                item("declination_override") {
                    SettingsPickerRow(
                        label = stringResource(R.string.compass_declination_override),
                        selected = getString(
                            R.string.degree_format,
                            declinationOverride
                        ),
                        enabled = !useAutoDeclination,
                        onClick = {
                            pickDeclinationOverride(declinationOverride) {
                                declinationOverride = it
                            }
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }
                item("declination_gps") {
                    SettingsActionRow(
                        label = stringResource(R.string.override_from_gps),
                        enabled = !useAutoDeclination,
                        onClick = ::updateDeclinationFromGps,
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val resetPrefs = listOf(
            getString(R.string.pref_use_true_north),
            getString(R.string.pref_compass_filter_amt),
            getString(R.string.pref_compass_source)
        )
        cache.onChange.asLiveData().observe(viewLifecycleOwner) {
            if (resetPrefs.contains(it)) {
                resetCompass()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startCompass()
        if (!gps.hasValidReading) {
            gps.start(this::onLocationUpdate)
        }
    }

    override fun onPause() {
        super.onPause()
        stopCompass()
        gps.stop(this::onLocationUpdate)
        gps.stop(this::onUpdateDeclinationFromGpsCallback)
    }

    override fun onDestroyView() {
        setCompassReadings = null
        setDeclinationOverride = null
        super.onDestroyView()
    }

    private fun onLocationUpdate(): Boolean {
        update()
        return false
    }

    private fun updateDeclinationFromGps() {
        if (gps.hasValidReading) {
            onUpdateDeclinationFromGpsCallback()
        } else {
            gps.start(this::onUpdateDeclinationFromGpsCallback)
        }
    }

    private fun getDeclination(): Float {
        return DeclinationFactory().getDeclinationStrategy(prefs, gps).getDeclination()
    }

    private fun onUpdateDeclinationFromGpsCallback(): Boolean {
        val declination = Geophysics.getGeomagneticDeclination(gps.location, gps.altitude)
        prefs.declinationOverride = declination
        setDeclinationOverride?.invoke(declination)
        update()
        Alerts.toast(requireContext(), getString(R.string.declination_override_updated_toast))
        return false
    }

    private fun resetCompass() {
        stopCompass()
        compass = sensorService.getCompass()
        startCompass()
    }

    private fun startCompass() {
        compass.start(this::onCompassUpdate)
    }

    private fun stopCompass() {
        compass.stop(this::onCompassUpdate)
    }

    private fun onCompassUpdate(): Boolean {
        update()
        return true
    }

    private fun update() {
        if (throttle.isThrottled()) {
            return
        }

        if (prevQuality != Quality.Unknown && prevQuality != compass.quality) {
            if (compass.quality.ordinal > prevQuality.ordinal) {
                Alerts.toast(
                    requireContext(),
                    getString(R.string.compass_accuracy_improved, getCompassAccuracy())
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

    private fun showCalibrationDialog() {
        dialog(
            getString(R.string.calibrate_compass_dialog_title),
            getString(
                R.string.calibrate_compass_dialog_content,
                getString(android.R.string.ok)
            ),
            contentView = if (hasCompass) compassCalibrationView(
                requireContext(),
                ViewGroup.LayoutParams.MATCH_PARENT,
                Resources.dp(requireContext(), 200f).toInt()
            ) else null,
            cancelText = null,
            cancelOnOutsideTouch = false,
            scrollable = true
        )
    }

    private fun pickOption(
        title: String,
        options: List<CompassOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        Pickers.item(
            requireContext(),
            title,
            options.map { it.label },
            options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        ) { index ->
            if (index != null) {
                onSelected(options[index].value)
            }
        }
    }

    private fun pickDeclinationOverride(current: Float, onPicked: (Float) -> Unit) {
        Pickers.number(
            requireContext(),
            getString(R.string.compass_declination_override),
            null,
            current,
            allowDecimals = true,
            allowNegative = true,
            hint = getString(R.string.declination)
        ) {
            if (it != null) {
                val declination = it.toFloat()
                prefs.declinationOverride = declination
                onPicked(declination)
                update()
            }
        }
    }

    private fun getCompassSourceOptions(): List<CompassOption> {
        val availableSources = CompassProvider.getAvailableSources(requireContext())
        return mapOf(
            CompassSource.RotationVector to getString(R.string.compass_source_mag_gyro),
            CompassSource.GeomagneticRotationVector to getString(R.string.magnetometer),
            CompassSource.CustomRotationVector to getString(R.string.magnetometer_gyro_custom),
            CompassSource.CustomMagnetometer to getString(R.string.compass_source_legacy_ts),
            CompassSource.Orientation to getString(R.string.compass_source_legacy_android)
        ).filter { availableSources.contains(it.key) }
            .map { CompassOption(it.key.id, it.value) }
    }

    private fun optionLabel(options: List<CompassOption>, selected: String): String {
        return options.firstOrNull { it.value == selected }?.label ?: selected
    }

    private fun getCompassAccuracy(): String {
        return formatService.formatQuality(compass.quality)
    }

    private fun getBoolean(key: Int, defaultValue: Boolean = false): Boolean {
        return cache.getBoolean(getString(key)) ?: defaultValue
    }

    private fun putString(key: Int, value: String) {
        cache.putString(getString(key), value)
    }

    private fun putInt(key: Int, value: Int) {
        cache.putInt(getString(key), value)
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
}

@Composable
private fun CompassSmoothingRow(
    smoothing: Int,
    onSmoothingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = TrailSenseSpacing.md)
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
