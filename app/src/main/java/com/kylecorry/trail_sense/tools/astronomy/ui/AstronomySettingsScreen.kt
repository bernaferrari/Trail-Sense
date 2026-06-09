@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.astronomy.ui

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.fragments.asLiveData
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsPickerRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunriseAlarmReceiver
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import java.time.Duration

@Composable
fun AstronomySettingsToolScreen(fragment: TrailSenseDestinationHost) {
    val controller = remember(fragment) { AstronomySettingsController(fragment) }
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

private class AstronomySettingsController(private val fragment: TrailSenseDestinationHost) {

    private val cache by lazy { PreferencesSubsystem.getInstance(fragment.requireContext()).preferences }
    private val prefs by lazy { UserPreferences(fragment.requireContext()) }
    private val formatter by lazy { FormatService.getInstance(fragment.requireContext()) }
    private val sunsetService by lazy {
        Tools.getService(
            fragment.requireContext(),
            AstronomyToolRegistration.SERVICE_SUNSET_ALERTS
        )!!
    }
    private val sunriseService by lazy {
        Tools.getService(
            fragment.requireContext(),
            AstronomyToolRegistration.SERVICE_SUNRISE_ALERTS
        )!!
    }

    private var setSunsetAlertsState: ((Boolean) -> Unit)? = null
    private var setSunriseAlertsState: ((Boolean) -> Unit)? = null

    @Composable
    fun Content() {
                    val sunTimeOptions = remember {
                        options(R.array.sun_times_entries, R.array.sun_times_values)
                    }
                    val quickActionOptions = remember {
                        Tools.getQuickActions(fragment.requireContext()).map {
                            AstronomyOption(it.id.toString(), it.name)
                        }
                    }
                    val sunsetTimeOptions = remember {
                        timeOptions(listOf(30L, 60L, 90L, 120L, 150L, 180L))
                    }
                    val sunriseTimeOptions = remember {
                        timeOptions(listOf(0L, 15L, 30L, 45L, 60L, 90L, 120L))
                    }
                    val showStartCameraIn3D = remember {
                        Tools.isToolAvailable(fragment.requireContext(), Tools.AUGMENTED_REALITY)
                    }

                    var sunTimeMode by remember {
                        mutableStateOf(getStringPreference(R.string.pref_sun_time_mode, "actual"))
                    }
                    var centerSunAndMoon by remember {
                        mutableStateOf(getBoolean(R.string.pref_center_sun_and_moon, false))
                    }
                    var showAstronomyBands by remember {
                        mutableStateOf(prefs.astronomy.showAstronomyBands)
                    }
                    var leftQuickAction by remember {
                        mutableStateOf(prefs.astronomy.leftButton.toString())
                    }
                    var rightQuickAction by remember {
                        mutableStateOf(prefs.astronomy.rightButton.toString())
                    }
                    var startCameraIn3D by remember {
                        mutableStateOf(
                            getBoolean(R.string.pref_start_camera_in_3d_view, true)
                        )
                    }
                    var sunsetAlerts by remember {
                        mutableStateOf(prefs.astronomy.sendSunsetAlerts)
                    }
                    var sunsetAlertTime by remember {
                        mutableStateOf(
                            getStringPreference(R.string.pref_sunset_alert_time, "60")
                        )
                    }
                    var useAlarmForSunset by remember {
                        mutableStateOf(
                            getBoolean(
                                R.string.pref_astronomy_use_alarm_for_sunset_alert,
                                false
                            )
                        )
                    }
                    var sunriseAlerts by remember {
                        mutableStateOf(prefs.astronomy.sendSunriseAlerts)
                    }
                    var sunriseAlertTime by remember {
                        mutableStateOf(
                            getStringPreference(R.string.pref_sunrise_alert_time, "0")
                        )
                    }
                    var useAlarmForSunrise by remember {
                        mutableStateOf(
                            getBoolean(
                                R.string.pref_astronomy_use_alarm_for_sunrise_alert,
                                false
                            )
                        )
                    }
                    var meteorAlerts by remember {
                        mutableStateOf(prefs.astronomy.sendMeteorShowerAlerts)
                    }
                    var lunarEclipseAlerts by remember {
                        mutableStateOf(prefs.astronomy.sendLunarEclipseAlerts)
                    }
                    var solarEclipseAlerts by remember {
                        mutableStateOf(prefs.astronomy.sendSolarEclipseAlerts)
                    }

                    setSunsetAlertsState = {
                        sunsetAlerts = prefs.astronomy.sendSunsetAlerts
                    }
                    setSunriseAlertsState = {
                        sunriseAlerts = prefs.astronomy.sendSunriseAlerts
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
                            item("general_header") {
                                TrailSenseSectionHeader(title = stringResource(R.string.general))
                            }
                            item("sun_time_mode") {
                                SettingsPickerRow(
                                    label = stringResource(R.string.pref_sun_time_mode_title),
                                    selected = optionLabel(sunTimeOptions, sunTimeMode),
                                    onClick = {
                                        pickOption(
                                            getString(R.string.pref_sun_time_mode_title),
                                            sunTimeOptions,
                                            sunTimeMode
                                        ) {
                                            sunTimeMode = it
                                            putString(R.string.pref_sun_time_mode, it)
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("center_sun_moon") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_center_sun_and_moon_title),
                                    checked = centerSunAndMoon,
                                    onCheckedChange = {
                                        centerSunAndMoon = it
                                        putBoolean(R.string.pref_center_sun_and_moon, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("astronomy_bands") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_show_astronomy_bands_title),
                                    checked = showAstronomyBands,
                                    onCheckedChange = {
                                        showAstronomyBands = it
                                        prefs.astronomy.showAstronomyBands = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("left_action") {
                                SettingsPickerRow(
                                    label = stringResource(R.string.left_quick_action),
                                    selected = optionLabel(quickActionOptions, leftQuickAction),
                                    onClick = {
                                        pickOption(
                                            getString(R.string.left_quick_action),
                                            quickActionOptions,
                                            leftQuickAction
                                        ) {
                                            leftQuickAction = it
                                            putString(
                                                R.string.pref_astronomy_quick_action_left,
                                                it
                                            )
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("right_action") {
                                SettingsPickerRow(
                                    label = stringResource(R.string.right_quick_action),
                                    selected = optionLabel(quickActionOptions, rightQuickAction),
                                    onClick = {
                                        pickOption(
                                            getString(R.string.right_quick_action),
                                            quickActionOptions,
                                            rightQuickAction
                                        ) {
                                            rightQuickAction = it
                                            putString(
                                                R.string.pref_astronomy_quick_action_right,
                                                it
                                            )
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            if (showStartCameraIn3D) {
                                item("start_camera_3d") {
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.pref_start_camera_in_3d_view_title),
                                        summary = stringResource(R.string.pref_start_camera_in_3d_view_summary),
                                        checked = startCameraIn3D,
                                        onCheckedChange = {
                                            startCameraIn3D = it
                                            putBoolean(
                                                R.string.pref_start_camera_in_3d_view,
                                                it
                                            )
                                        },
                                        modifier = Modifier.padding(
                                            horizontal = TrailSenseSpacing.lg
                                        )
                                    )
                                    TrailSenseDivider()
                                }
                            }
                            item("alerts_header") {
                                TrailSenseSectionHeader(title = stringResource(R.string.alerts))
                            }
                            item("sunset_alerts") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.sunset),
                                    checked = sunsetAlerts,
                                    onCheckedChange = {
                                        sunsetAlerts = it
                                        prefs.astronomy.sendSunsetAlerts = it
                                        onSunsetAlertsPreferenceChanged(it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("sunset_alert_time") {
                                SettingsPickerRow(
                                    label = stringResource(R.string.pref_sunset_alert_time_title),
                                    selected = optionLabel(sunsetTimeOptions, sunsetAlertTime),
                                    enabled = sunsetAlerts,
                                    onClick = {
                                        pickOption(
                                            getString(R.string.pref_sunset_alert_time_title),
                                            sunsetTimeOptions,
                                            sunsetAlertTime
                                        ) {
                                            sunsetAlertTime = it
                                            putString(R.string.pref_sunset_alert_time, it)
                                            restartSunsetAlerts()
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("sunset_alarm") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.use_alarm_for_sunset_alert),
                                    summary = stringResource(R.string.alarm_description),
                                    checked = useAlarmForSunset,
                                    onCheckedChange = {
                                        useAlarmForSunset = it
                                        putBoolean(
                                            R.string.pref_astronomy_use_alarm_for_sunset_alert,
                                            it
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("sunrise_alerts") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.sunrise),
                                    checked = sunriseAlerts,
                                    onCheckedChange = {
                                        sunriseAlerts = it
                                        prefs.astronomy.sendSunriseAlerts = it
                                        onSunriseAlertsPreferenceChanged(it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("sunrise_alert_time") {
                                SettingsPickerRow(
                                    label = stringResource(R.string.pref_sunrise_alert_time_title),
                                    selected = optionLabel(sunriseTimeOptions, sunriseAlertTime),
                                    enabled = sunriseAlerts,
                                    onClick = {
                                        pickOption(
                                            getString(R.string.pref_sunrise_alert_time_title),
                                            sunriseTimeOptions,
                                            sunriseAlertTime
                                        ) {
                                            sunriseAlertTime = it
                                            putString(R.string.pref_sunrise_alert_time, it)
                                            restartSunriseAlerts()
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("sunrise_alarm") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.use_alarm_for_sunrise_alert),
                                    summary = stringResource(R.string.alarm_description),
                                    checked = useAlarmForSunrise,
                                    onCheckedChange = {
                                        useAlarmForSunrise = it
                                        putBoolean(
                                            R.string.pref_astronomy_use_alarm_for_sunrise_alert,
                                            it
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("meteor_alerts") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_meteor_showers_title),
                                    checked = meteorAlerts,
                                    onCheckedChange = {
                                        meteorAlerts = it
                                        prefs.astronomy.sendMeteorShowerAlerts = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("lunar_alerts") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.lunar_eclipses),
                                    checked = lunarEclipseAlerts,
                                    onCheckedChange = {
                                        lunarEclipseAlerts = it
                                        prefs.astronomy.sendLunarEclipseAlerts = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("solar_alerts") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.solar_eclipses),
                                    checked = solarEclipseAlerts,
                                    onCheckedChange = {
                                        solarEclipseAlerts = it
                                        prefs.astronomy.sendSolarEclipseAlerts = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                        }
                    }
    }

    fun onCreate() {
        val sunsetAlertTimeKey = getString(R.string.pref_sunset_alert_time)
        val sunriseAlertTimeKey = getString(R.string.pref_sunrise_alert_time)

        cache.onChange.asLiveData().observe(fragment.lifecycleOwner) {
            if (it == sunsetAlertTimeKey) {
                restartSunsetAlerts()
            } else if (it == sunriseAlertTimeKey) {
                restartSunriseAlerts()
            }
        }
    }

    fun onResume() {
        Tools.subscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_ENABLED,
            ::onSunsetAlertsChanged
        )
        Tools.subscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_DISABLED,
            ::onSunsetAlertsChanged
        )
        Tools.subscribe(
            AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_ENABLED,
            ::onSunriseAlertsChanged
        )
        Tools.subscribe(
            AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_DISABLED,
            ::onSunriseAlertsChanged
        )
    }

    fun onPause() {
        Tools.unsubscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_ENABLED,
            ::onSunsetAlertsChanged
        )
        Tools.unsubscribe(
            AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_DISABLED,
            ::onSunsetAlertsChanged
        )
        Tools.unsubscribe(
            AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_ENABLED,
            ::onSunriseAlertsChanged
        )
        Tools.unsubscribe(
            AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_DISABLED,
            ::onSunriseAlertsChanged
        )
    }

    fun onDestroy() {
        setSunsetAlertsState = null
        setSunriseAlertsState = null
    }

    private fun onSunsetAlertsPreferenceChanged(enabled: Boolean) {
        fragment.inBackground {
            if (enabled) {
                SunsetAlarmReceiver.enable(fragment, true)
            } else {
                sunsetService.disable()
            }
        }
    }

    private fun onSunriseAlertsPreferenceChanged(enabled: Boolean) {
        fragment.inBackground {
            if (enabled) {
                SunriseAlarmReceiver.enable(fragment, true)
            } else {
                sunriseService.disable()
            }
        }
    }

    private fun restartSunsetAlerts() {
        fragment.inBackground {
            sunsetService.restart()
        }
    }

    private fun restartSunriseAlerts() {
        fragment.inBackground {
            sunriseService.restart()
        }
    }

    private fun pickOption(
        title: String,
        options: List<AstronomyOption>,
        selected: String,
        onSelected: (String) -> Unit
    ) {
        Pickers.item(
            fragment.requireContext(),
            title,
            options.map { it.label },
            options.indexOfFirst { it.value == selected }.coerceAtLeast(0)
        ) { index ->
            if (index != null) {
                onSelected(options[index].value)
            }
        }
    }

    private fun options(entriesId: Int, valuesId: Int): List<AstronomyOption> {
        val entries = fragment.getResources().getStringArray(entriesId)
        val values = fragment.getResources().getStringArray(valuesId)
        return values.mapIndexed { index, value ->
            AstronomyOption(value, entries.getOrElse(index) { value })
        }
    }

    private fun timeOptions(minutes: List<Long>): List<AstronomyOption> {
        return minutes.map {
            AstronomyOption(
                it.toString(),
                formatter.formatDuration(Duration.ofMinutes(it))
            )
        }
    }

    private fun optionLabel(options: List<AstronomyOption>, selected: String): String {
        return options.firstOrNull { it.value == selected }?.label ?: selected
    }

    private fun getStringPreference(key: Int, default: String): String {
        return cache.getString(getString(key)) ?: default
    }

    private fun getBoolean(key: Int, default: Boolean): Boolean {
        return cache.getBoolean(getString(key)) ?: default
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

    private fun onSunsetAlertsChanged(data: Bundle): Boolean {
        setSunsetAlertsState?.invoke(prefs.astronomy.sendSunsetAlerts)
        return true
    }

    private fun onSunriseAlertsChanged(data: Bundle): Boolean {
        setSunriseAlertsState?.invoke(prefs.astronomy.sendSunriseAlerts)
        return true
    }

    private data class AstronomyOption(
        val value: String,
        val label: String
    )
}
