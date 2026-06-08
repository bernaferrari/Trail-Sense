@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.weather.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.fragments.inBackground
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.pickers.Pickers
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsInfoRow
import com.kylecorry.trail_sense.settings.ui.SettingsPickerRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.permissions.RemoveBatteryRestrictionsAlerter
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.requireMainActivity
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.infrastructure.observe
import com.kylecorry.trail_sense.tools.weather.WeatherToolRegistration
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherCsvConverter
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherPreferences
import com.kylecorry.trail_sense.tools.weather.infrastructure.alerts.CurrentWeatherAlerter
import com.kylecorry.trail_sense.tools.weather.infrastructure.commands.ChangeWeatherFrequencyCommand
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.WeatherRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalTime

@Composable
fun WeatherSettingsToolScreen(fragment: Fragment) {
    val controller = remember(fragment) { WeatherSettingsController(fragment) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller, lifecycleOwner) {
        controller.onCreate()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                controller.onResume()
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

private class WeatherSettingsController(private val fragment: Fragment) {

    private val cache by lazy { PreferencesSubsystem.getInstance(fragment.requireContext()).preferences }
    private val userPrefs by lazy { UserPreferences(fragment.requireContext()) }
    private val prefs by lazy { userPrefs.weather }
    private val formatService by lazy { FormatService.getInstance(fragment.requireContext()) }

    private val weatherMonitorService by lazy {
        Tools.getService(
            fragment.requireContext(),
            WeatherToolRegistration.SERVICE_WEATHER_MONITOR
        )!!
    }

    private var setMonitorWeatherState: ((Boolean, Boolean) -> Unit)? = null
    private var refreshNotificationSummary: ((String) -> Unit)? = null

    @Composable
    fun Content() {
                    val quickActionOptions = remember {
                        Tools.getQuickActions(fragment.requireContext()).map {
                            WeatherOption(it.id.toString(), it.name)
                        }
                    }
                    val pressureHistoryOptions = remember {
                        options(R.array.pressure_history_entries, R.array.pressure_history_values)
                    }
                    val forecastSensitivityOptions = remember {
                        options(
                            getForecastSensitivities(userPrefs.pressureUnits),
                            R.array.forecast_sensitivity_values
                        )
                    }
                    val stormSensitivityOptions = remember {
                        options(
                            getStormSensitivities(userPrefs.pressureUnits),
                            R.array.storm_alert_sensitivity_values
                        )
                    }
                    val muteStormAlarmSummary = remember {
                        getString(
                            R.string.alarm_time_range,
                            formatService.formatTime(
                                LocalTime.of(8, 0),
                                includeSeconds = false,
                                includeMinutes = false
                            ),
                            formatService.formatTime(
                                LocalTime.of(20, 0),
                                includeSeconds = false,
                                includeMinutes = false
                            )
                        )
                    }

                    var leftQuickAction by remember {
                        mutableStateOf(prefs.leftButton.toString())
                    }
                    var rightQuickAction by remember {
                        mutableStateOf(prefs.rightButton.toString())
                    }
                    var pressureHistory by remember {
                        mutableStateOf(getStringPreference(R.string.pref_pressure_history, "48"))
                    }
                    var monitorWeather by remember {
                        mutableStateOf(getBoolean(R.string.pref_monitor_weather, true))
                    }
                    var monitorWeatherEnabled by remember {
                        mutableStateOf(!weatherMonitorService.isBlocked())
                    }
                    var forecastSensitivity by remember {
                        mutableStateOf(
                            getStringPreference(R.string.pref_forecast_sensitivity, "medium")
                        )
                    }
                    var notificationSummary by remember {
                        mutableStateOf(notificationSummary())
                    }
                    var showPressureInNotification by remember {
                        mutableStateOf(
                            getBoolean(R.string.pref_show_pressure_in_notification, false)
                        )
                    }
                    var showTemperatureInNotification by remember {
                        mutableStateOf(
                            getBoolean(R.string.pref_show_temperature_in_notification, true)
                        )
                    }
                    var dailyWeatherNotification by remember {
                        mutableStateOf(
                            getBoolean(R.string.pref_daily_weather_notification, true)
                        )
                    }
                    var dailyWeatherTime by remember {
                        mutableStateOf(prefs.dailyForecastTime)
                    }
                    var stormAlerts by remember {
                        mutableStateOf(getBoolean(R.string.pref_send_storm_alert, true))
                    }
                    var stormSensitivity by remember {
                        mutableStateOf(
                            getStringPreference(R.string.pref_storm_alert_sensitivity, "medium")
                        )
                    }
                    var useAlarmForStormAlert by remember {
                        mutableStateOf(
                            getBoolean(R.string.pref_weather_use_alarm_for_storm_alert, false)
                        )
                    }
                    var muteStormAlarmAtNight by remember {
                        mutableStateOf(
                            getBoolean(R.string.pref_weather_mute_storm_alarm_at_night, true)
                        )
                    }
                    var updateFrequency by remember {
                        mutableStateOf(prefs.weatherUpdateFrequency)
                    }
                    var showUpdateFrequencyPicker by remember { mutableStateOf(false) }
                    val updateFrequencyTitle =
                        stringResource(R.string.pref_weather_update_frequency_title)
                    var showDetailedIcon by remember {
                        mutableStateOf(
                            getBoolean(R.string.pref_weather_show_detailed_icon, true)
                        )
                    }
                    var debugTimingSummary by remember { mutableStateOf<String?>(null) }

                    setMonitorWeatherState = { enabled, available ->
                        monitorWeather = enabled
                        monitorWeatherEnabled = available
                    }
                    refreshNotificationSummary = {
                        notificationSummary = it
                    }

                    if (isDebug()) {
                        LaunchedEffect(Unit) {
                            debugTimingSummary = getDebugTimingSummary()
                        }
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
                                            putString(R.string.pref_weather_quick_action_left, it)
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
                                            putString(R.string.pref_weather_quick_action_right, it)
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("pressure_history") {
                                SettingsPickerRow(
                                    label = stringResource(R.string.pref_pressure_history_title),
                                    selected = optionLabel(pressureHistoryOptions, pressureHistory),
                                    onClick = {
                                        pickOption(
                                            getString(R.string.pref_pressure_history_title),
                                            pressureHistoryOptions,
                                            pressureHistory
                                        ) {
                                            pressureHistory = it
                                            putString(R.string.pref_pressure_history, it)
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("forecast_header") {
                                TrailSenseSectionHeader(
                                    title = stringResource(R.string.forecasts_category)
                                )
                            }
                            item("monitor_weather") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_monitor_weather_title),
                                    summary = stringResource(R.string.pref_monitor_weather_summary),
                                    checked = monitorWeather,
                                    enabled = monitorWeatherEnabled,
                                    onCheckedChange = {
                                        monitorWeather = it
                                        prefs.shouldMonitorWeather = it
                                        onMonitorWeatherChanged(it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("forecast_sensitivity") {
                                SettingsPickerRow(
                                    label = stringResource(R.string.pref_forecast_sensitivity_title),
                                    selected = optionLabel(
                                        forecastSensitivityOptions,
                                        forecastSensitivity
                                    ),
                                    onClick = {
                                        pickOption(
                                            getString(R.string.pref_forecast_sensitivity_title),
                                            forecastSensitivityOptions,
                                            forecastSensitivity
                                        ) {
                                            forecastSensitivity = it
                                            putString(R.string.pref_forecast_sensitivity, it)
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("notifications") {
                                SettingsActionRow(
                                    label = getString(
                                        R.string.notifications_channel,
                                        getString(R.string.weather_monitor)
                                    ),
                                    summary = notificationSummary,
                                    onClick = {
                                        fragment.startActivity(
                                            Intents.notificationSettings(
                                                fragment.requireContext(),
                                                CurrentWeatherAlerter.WEATHER_CHANNEL_ID
                                            )
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("show_pressure") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_show_pressure_in_notification_title),
                                    checked = showPressureInNotification,
                                    onCheckedChange = {
                                        showPressureInNotification = it
                                        putBoolean(
                                            R.string.pref_show_pressure_in_notification,
                                            it
                                        )
                                        restartWeatherMonitor()
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("show_temperature") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_show_temperature_in_notification_title),
                                    checked = showTemperatureInNotification,
                                    onCheckedChange = {
                                        showTemperatureInNotification = it
                                        putBoolean(
                                            R.string.pref_show_temperature_in_notification,
                                            it
                                        )
                                        restartWeatherMonitor()
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("daily_weather") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_daily_weather_notification_title),
                                    checked = dailyWeatherNotification,
                                    enabled = monitorWeather,
                                    onCheckedChange = {
                                        dailyWeatherNotification = it
                                        putBoolean(
                                            R.string.pref_daily_weather_notification,
                                            it
                                        )
                                        restartWeatherMonitor()
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("daily_weather_time") {
                                SettingsPickerRow(
                                    label = stringResource(R.string.pref_daily_weather_time_title),
                                    selected = formatService.formatTime(dailyWeatherTime, false),
                                    enabled = dailyWeatherNotification,
                                    onClick = {
                                        Pickers.time(
                                            fragment.requireContext(),
                                            userPrefs.use24HourTime,
                                            dailyWeatherTime
                                        ) { time ->
                                            if (time != null) {
                                                dailyWeatherTime = time
                                                prefs.dailyForecastTime = time
                                                restartWeatherMonitor()
                                            }
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("storm_header") {
                                TrailSenseSectionHeader(
                                    title = stringResource(R.string.weather_storm)
                                )
                            }
                            item("storm_alerts") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.storm_alerts),
                                    checked = stormAlerts,
                                    enabled = monitorWeather,
                                    onCheckedChange = {
                                        stormAlerts = it
                                        putBoolean(R.string.pref_send_storm_alert, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("storm_sensitivity") {
                                SettingsPickerRow(
                                    label = stringResource(R.string.pref_storm_alert_sensitivity_title),
                                    selected = optionLabel(stormSensitivityOptions, stormSensitivity),
                                    onClick = {
                                        pickOption(
                                            getString(R.string.pref_storm_alert_sensitivity_title),
                                            stormSensitivityOptions,
                                            stormSensitivity
                                        ) {
                                            stormSensitivity = it
                                            putString(R.string.pref_storm_alert_sensitivity, it)
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("storm_alarm") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.use_alarm_for_storm_alert),
                                    summary = stringResource(R.string.alarm_description),
                                    checked = useAlarmForStormAlert,
                                    enabled = stormAlerts,
                                    onCheckedChange = {
                                        useAlarmForStormAlert = it
                                        putBoolean(
                                            R.string.pref_weather_use_alarm_for_storm_alert,
                                            it
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("mute_storm_alarm") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.mute_alarm_at_night),
                                    summary = muteStormAlarmSummary,
                                    checked = muteStormAlarmAtNight,
                                    enabled = useAlarmForStormAlert,
                                    onCheckedChange = {
                                        muteStormAlarmAtNight = it
                                        putBoolean(
                                            R.string.pref_weather_mute_storm_alarm_at_night,
                                            it
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("advanced_header") {
                                TrailSenseSectionHeader(title = stringResource(R.string.advanced))
                            }
                            item("update_frequency") {
                                SettingsActionRow(
                                    label = updateFrequencyTitle,
                                    summary = formatService.formatDuration(updateFrequency),
                                    enabled = monitorWeather,
                                    onClick = {
                                        showUpdateFrequencyPicker = true
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            item("detailed_icon") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_weather_show_detailed_icon_title),
                                    checked = showDetailedIcon,
                                    enabled = monitorWeather,
                                    onCheckedChange = {
                                        showDetailedIcon = it
                                        putBoolean(R.string.pref_weather_show_detailed_icon, it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("export_csv") {
                                SettingsActionRow(
                                    label = stringResource(R.string.pref_export_weather_csv_title),
                                    onClick = ::exportWeatherData,
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                            }
                            if (isDebug()) {
                                item("debug_timing") {
                                    SettingsInfoRow(
                                        label = "Timing (Debug)",
                                        summary = debugTimingSummary,
                                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                    )
                            }
                        }
                    }

                    if (showUpdateFrequencyPicker) {
                        DurationPickerDialog(
                            title = updateFrequencyTitle,
                            duration = updateFrequency,
                            message = stringResource(R.string.actual_frequency_disclaimer),
                            hint = stringResource(R.string.frequency),
                            onDurationPicked = {
                                if (it != null) {
                                    ChangeWeatherFrequencyCommand(fragment.requireContext()) { frequency ->
                                        updateFrequency = frequency
                                    }.execute(it)
                                }
                                showUpdateFrequencyPicker = false
                            },
                            onDismiss = {
                                showUpdateFrequencyPicker = false
                            }
                        )
                    }
                }
    }

    fun onCreate() {
        Tools.observe(fragment, WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_STATE_CHANGED) {
            setMonitorWeatherState?.invoke(
                weatherMonitorService.isEnabled(),
                !weatherMonitorService.isBlocked()
            )
            true
        }
    }

    fun onResume() {
        refreshNotificationSummary?.invoke(notificationSummary())
    }

    fun onDestroy() {
        setMonitorWeatherState = null
        refreshNotificationSummary = null
    }

    private fun onMonitorWeatherChanged(enabled: Boolean) {
        fragment.inBackground {
            if (enabled && prefs.shouldMonitorWeather) {
                weatherMonitorService.enable()
                RequestRemoveBatteryRestrictionCommand(
                    fragment.requireContext(),
                    RemoveBatteryRestrictionsAlerter(fragment)
                ).execute()
            } else {
                weatherMonitorService.disable()
            }
        }
    }

    private fun restartWeatherMonitor() {
        fragment.inBackground {
            weatherMonitorService.restart()
        }
    }

    private fun pickOption(
        title: String,
        options: List<WeatherOption>,
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

    private fun options(entriesId: Int, valuesId: Int): List<WeatherOption> {
        return options(fragment.resources.getStringArray(entriesId).toList(), valuesId)
    }

    private fun options(entries: List<CharSequence>, valuesId: Int): List<WeatherOption> {
        val values = fragment.resources.getStringArray(valuesId)
        return values.mapIndexed { index, value ->
            WeatherOption(value, entries.getOrElse(index) { value }.toString())
        }
    }

    private fun optionLabel(options: List<WeatherOption>, selected: String): String {
        return options.firstOrNull { it.value == selected }?.label ?: selected
    }

    private fun getForecastSensitivities(units: PressureUnits): List<CharSequence> {
        val hpa = listOf(
            Pressure.from(WeatherPreferences.HPA_FORECAST_LOW, PressureUnits.Hpa),
            Pressure.from(WeatherPreferences.HPA_FORECAST_MEDIUM, PressureUnits.Hpa),
            Pressure.from(WeatherPreferences.HPA_FORECAST_HIGH, PressureUnits.Hpa),
        )

        val stringValues = hpa.map {
            getString(
                R.string.pressure_tendency_format_2,
                formatService.formatPressure(
                    it.convertTo(units),
                    Units.getDecimalPlaces(units) + 1,
                    false
                )
            )
        }

        return listOf(
            getString(R.string.low_amount, stringValues[0]),
            getString(R.string.medium_amount, stringValues[1]),
            getString(R.string.high_amount, stringValues[2])
        )
    }

    private fun getStormSensitivities(units: PressureUnits): List<CharSequence> {
        val hpa = listOf(
            Pressure.from(WeatherPreferences.HPA_STORM_LOW, PressureUnits.Hpa),
            Pressure.from(WeatherPreferences.HPA_STORM_MEDIUM, PressureUnits.Hpa),
            Pressure.from(WeatherPreferences.HPA_STORM_HIGH, PressureUnits.Hpa),
        )

        val stringValues = hpa.map {
            getString(
                R.string.pressure_tendency_format_2,
                formatService.formatPressure(
                    it.convertTo(units),
                    Units.getDecimalPlaces(units) + 1,
                    false
                )
            )
        }

        return listOf(
            getString(R.string.low_amount, stringValues[0]),
            getString(R.string.medium_amount, stringValues[1]),
            getString(R.string.high_amount, stringValues[2])
        )
    }

    private suspend fun getDebugTimingSummary(): String? {
        return withContext(Dispatchers.Default) {
            val readings = WeatherRepo.getInstance(fragment.requireContext()).getAll()
                .zipWithNext { a, b -> Duration.between(a.time, b.time).seconds / 60f }

            if (readings.isEmpty()) {
                return@withContext null
            }

            val mean = Statistics.mean(readings).safeRoundToInt()
            val stdev = Statistics.stdev(readings, mean = mean.toFloat()).safeRoundToInt()
            val max = readings.maxOrNull()?.safeRoundToInt() ?: 0
            val median = Statistics.median(readings).safeRoundToInt()
            val quantile75 = Statistics.quantile(readings, 0.75f).safeRoundToInt()
            val quantile90 = Statistics.quantile(readings, 0.9f).safeRoundToInt()

            "Mean: $mean\n" +
                    "Stdev: $stdev\n" +
                    "Max: $max\n" +
                    "Median: $median\n" +
                    "75th: $quantile75\n" +
                    "90th: $quantile90"
        }
    }

    private fun exportWeatherData() {
        val exporter = IOFactory().createCsvService(fragment.requireMainActivity())
        val repo = WeatherRepo.getInstance(fragment.requireContext())
        fragment.inBackground(BackgroundMinimumState.Created) {
            val exported = withContext(Dispatchers.IO) {
                val readings = repo.getAll().sortedByDescending { it.time }
                val csv = WeatherCsvConverter().toCSV(readings)
                exporter.export(csv, "weather-${Instant.now().toEpochMilli()}.csv")
            }
            if (exported) {
                withContext(Dispatchers.Main) {
                    Alerts.toast(fragment.requireContext(), getString(R.string.weather_exported))
                }
            }
        }
    }

    private fun notificationSummary(): String {
        val isBlocked = Notify.isChannelBlocked(
            fragment.requireContext(),
            CurrentWeatherAlerter.WEATHER_CHANNEL_ID
        )
        return if (isBlocked) getString(R.string.off) else getString(R.string.on)
    }

    private fun getString(@StringRes id: Int, vararg args: Any): String {
        return fragment.getString(id, *args)
    }

    private fun getStringPreference(key: Int, default: String): String {
        return cache.getString(getString(key)) ?: default
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

    private data class WeatherOption(
        val value: String,
        val label: String
    )
}
