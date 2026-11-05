@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.weather.ui

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.ScreenEffects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.system.Intents
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.io.IOFactory
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsInfoRow
import com.kylecorry.trail_sense.settings.ui.SettingsPickerRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
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
fun WeatherSettingsToolScreen() {
    val context = LocalContext.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    // MainActivity is the app's IntentResultRetriever, which the CSV export's file picker needs.
    val intentResultRetriever = LocalActivity.current as IntentResultRetriever
    val controller = remember(context) {
        WeatherSettingsController(context, scope, effects, intentResultRetriever)
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    ToolBroadcastEffect(WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_STATE_CHANGED) {
        controller.onMonitorStateChanged()
    }

    DisposableEffect(controller, lifecycleOwner) {
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

private class WeatherSettingsController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val effects: ScreenEffects,
    private val intentResultRetriever: IntentResultRetriever
) {

    private val cache by lazy { PreferencesSubsystem.getInstance(context).preferences }
    private val userPrefs by lazy { UserPreferences(context) }
    private val prefs by lazy { userPrefs.weather }
    private val formatService by lazy { FormatService.getInstance(context) }

    private val weatherMonitorService by lazy {
        Tools.getService(
            context,
            WeatherToolRegistration.SERVICE_WEATHER_MONITOR
        )!!
    }

    private var setMonitorWeatherState: ((Boolean, Boolean) -> Unit)? = null
    private var refreshNotificationSummary: ((String) -> Unit)? = null

    @Composable
    fun Content() {
        val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        val quickActionOptions = remember {
            Tools.getQuickActions(context).map { WeatherOption(it.id.toString(), it.name) }
        }
        val pressureHistoryOptions = remember {
            options(R.array.pressure_history_entries, R.array.pressure_history_values)
        }
        val forecastSensitivityOptions = remember {
            options(getForecastSensitivities(userPrefs.pressureUnits), R.array.forecast_sensitivity_values)
        }
        val stormSensitivityOptions = remember {
            options(getStormSensitivities(userPrefs.pressureUnits), R.array.storm_alert_sensitivity_values)
        }
        val muteStormAlarmSummary = remember {
            getString(
                R.string.alarm_time_range,
                formatService.formatTime(LocalTime.of(8, 0), includeSeconds = false, includeMinutes = false),
                formatService.formatTime(LocalTime.of(20, 0), includeSeconds = false, includeMinutes = false)
            )
        }

        var leftQuickAction by remember { mutableStateOf(prefs.leftButton.toString()) }
        var rightQuickAction by remember { mutableStateOf(prefs.rightButton.toString()) }
        var pressureHistory by remember {
            mutableStateOf(getStringPreference(R.string.pref_pressure_history, "48"))
        }
        var monitorWeather by remember { mutableStateOf(getBoolean(R.string.pref_monitor_weather, true)) }
        var monitorWeatherEnabled by remember { mutableStateOf(!weatherMonitorService.isBlocked()) }
        var forecastSensitivity by remember {
            mutableStateOf(getStringPreference(R.string.pref_forecast_sensitivity, "medium"))
        }
        var notificationSummary by remember { mutableStateOf(notificationSummary()) }
        var showPressureInNotification by remember {
            mutableStateOf(getBoolean(R.string.pref_show_pressure_in_notification, false))
        }
        var showTemperatureInNotification by remember {
            mutableStateOf(getBoolean(R.string.pref_show_temperature_in_notification, true))
        }
        var dailyWeatherNotification by remember {
            mutableStateOf(getBoolean(R.string.pref_daily_weather_notification, true))
        }
        var dailyWeatherTime by remember { mutableStateOf(prefs.dailyForecastTime) }
        var stormAlerts by remember { mutableStateOf(getBoolean(R.string.pref_send_storm_alert, true)) }
        var stormSensitivity by remember {
            mutableStateOf(getStringPreference(R.string.pref_storm_alert_sensitivity, "medium"))
        }
        var useAlarmForStormAlert by remember {
            mutableStateOf(getBoolean(R.string.pref_weather_use_alarm_for_storm_alert, false))
        }
        var muteStormAlarmAtNight by remember {
            mutableStateOf(getBoolean(R.string.pref_weather_mute_storm_alarm_at_night, true))
        }
        var updateFrequency by remember { mutableStateOf(prefs.weatherUpdateFrequency) }
        var showUpdateFrequencyPicker by remember { mutableStateOf(false) }
        val updateFrequencyTitle = stringResource(R.string.pref_weather_update_frequency_title)
        var showDetailedIcon by remember {
            mutableStateOf(getBoolean(R.string.pref_weather_show_detailed_icon, true))
        }
        var debugTimingSummary by remember { mutableStateOf<String?>(null) }

        setMonitorWeatherState = { enabled, available ->
            monitorWeather = enabled
            monitorWeatherEnabled = available
        }
        refreshNotificationSummary = { notificationSummary = it }

        if (isDebug()) {
            LaunchedEffect(Unit) {
                debugTimingSummary = getDebugTimingSummary()
            }
        }

        val generalTitle = stringResource(R.string.general)
        val forecastTitle = stringResource(R.string.forecasts_category)
        val notificationTitle = stringResource(R.string.settings_notifications_section)
        val stormTitle = stringResource(R.string.weather_storm)
        val advancedTitle = stringResource(R.string.advanced)
        val notificationChannelTitle = getString(R.string.notifications_channel, getString(R.string.weather_monitor))

        TrailSensePage(
            title = stringResource(R.string.weather),
            onBack = { dispatcher?.onBackPressed() },
            backLabel = stringResource(R.string.back)
        ) {
            trailSenseSection("general", title = generalTitle) {
                row { position ->
                    SettingsPickerRow(
                        label = stringResource(R.string.left_quick_action),
                        selected = optionLabel(quickActionOptions, leftQuickAction),
                        onClick = {
                            pickOption(getString(R.string.left_quick_action), quickActionOptions, leftQuickAction) {
                                leftQuickAction = it
                                putString(R.string.pref_weather_quick_action_left, it)
                            }
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsPickerRow(
                        label = stringResource(R.string.right_quick_action),
                        selected = optionLabel(quickActionOptions, rightQuickAction),
                        onClick = {
                            pickOption(getString(R.string.right_quick_action), quickActionOptions, rightQuickAction) {
                                rightQuickAction = it
                                putString(R.string.pref_weather_quick_action_right, it)
                            }
                        },
                        position = position
                    )
                }
                row { position ->
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
                        position = position
                    )
                }
            }

            trailSenseSection("forecast", title = forecastTitle) {
                if (monitorWeatherEnabled) {
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.pref_monitor_weather_title),
                            summary = stringResource(R.string.pref_monitor_weather_summary),
                            checked = monitorWeather,
                            onCheckedChange = {
                                monitorWeather = it
                                prefs.shouldMonitorWeather = it
                                onMonitorWeatherChanged(it)
                            },
                            position = position
                        )
                    }
                }
                row { position ->
                    SettingsPickerRow(
                        label = stringResource(R.string.pref_forecast_sensitivity_title),
                        selected = optionLabel(forecastSensitivityOptions, forecastSensitivity),
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
                        position = position
                    )
                }
            }

            trailSenseSection("notification", title = notificationTitle) {
                row { position ->
                    SettingsPickerRow(
                        label = notificationChannelTitle,
                        selected = notificationSummary,
                        onClick = {
                            context.startActivity(
                                Intents.notificationSettings(
                                    context,
                                    CurrentWeatherAlerter.WEATHER_CHANNEL_ID
                                )
                            )
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_show_pressure_in_notification_title),
                        checked = showPressureInNotification,
                        onCheckedChange = {
                            showPressureInNotification = it
                            putBoolean(R.string.pref_show_pressure_in_notification, it)
                            restartWeatherMonitor()
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_show_temperature_in_notification_title),
                        checked = showTemperatureInNotification,
                        onCheckedChange = {
                            showTemperatureInNotification = it
                            putBoolean(R.string.pref_show_temperature_in_notification, it)
                            restartWeatherMonitor()
                        },
                        position = position
                    )
                }
                if (monitorWeather) {
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.pref_daily_weather_notification_title),
                            checked = dailyWeatherNotification,
                            onCheckedChange = {
                                dailyWeatherNotification = it
                                putBoolean(R.string.pref_daily_weather_notification, it)
                                restartWeatherMonitor()
                            },
                            position = position
                        )
                    }
                    if (dailyWeatherNotification) {
                        row { position ->
                            SettingsPickerRow(
                                label = stringResource(R.string.pref_daily_weather_time_title),
                                selected = formatService.formatTime(dailyWeatherTime, false),
                                onClick = {
                                    Pickers.time(
                                        context,
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
                                position = position
                            )
                        }
                    }
                }
            }

            trailSenseSection("storm", title = stormTitle) {
                if (monitorWeather) {
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.storm_alerts),
                            checked = stormAlerts,
                            onCheckedChange = {
                                stormAlerts = it
                                putBoolean(R.string.pref_send_storm_alert, it)
                            },
                            position = position
                        )
                    }
                }
                row { position ->
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
                        position = position
                    )
                }
                if (monitorWeather && stormAlerts) {
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.use_alarm_for_storm_alert),
                            summary = stringResource(R.string.alarm_description),
                            checked = useAlarmForStormAlert,
                            onCheckedChange = {
                                useAlarmForStormAlert = it
                                putBoolean(R.string.pref_weather_use_alarm_for_storm_alert, it)
                            },
                            position = position
                        )
                    }
                    if (useAlarmForStormAlert) {
                        row { position ->
                            SettingsSwitchRow(
                                label = stringResource(R.string.mute_alarm_at_night),
                                summary = muteStormAlarmSummary,
                                checked = muteStormAlarmAtNight,
                                onCheckedChange = {
                                    muteStormAlarmAtNight = it
                                    putBoolean(R.string.pref_weather_mute_storm_alarm_at_night, it)
                                },
                                position = position
                            )
                        }
                    }
                }
            }

            trailSenseSection("advanced", title = advancedTitle) {
                if (monitorWeather) {
                    row { position ->
                        SettingsPickerRow(
                            label = updateFrequencyTitle,
                            selected = formatService.formatDuration(updateFrequency),
                            onClick = { showUpdateFrequencyPicker = true },
                            position = position
                        )
                    }
                    row { position ->
                        SettingsSwitchRow(
                            label = stringResource(R.string.pref_weather_show_detailed_icon_title),
                            checked = showDetailedIcon,
                            onCheckedChange = {
                                showDetailedIcon = it
                                putBoolean(R.string.pref_weather_show_detailed_icon, it)
                            },
                            position = position
                        )
                    }
                }
                row { position ->
                    SettingsActionRow(
                        label = stringResource(R.string.pref_export_weather_csv_title),
                        onClick = ::exportWeatherData,
                        position = position
                    )
                }
                if (isDebug()) {
                    row { position ->
                        SettingsInfoRow(
                            label = "Timing (Debug)",
                            summary = debugTimingSummary,
                            position = position
                        )
                    }
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
                        ChangeWeatherFrequencyCommand(context) { frequency ->
                            updateFrequency = frequency
                        }.execute(it)
                    }
                    showUpdateFrequencyPicker = false
                },
                onDismiss = { showUpdateFrequencyPicker = false }
            )
        }
    }

    fun onMonitorStateChanged() {
        setMonitorWeatherState?.invoke(
            weatherMonitorService.isEnabled(),
            !weatherMonitorService.isBlocked()
        )
    }

    fun onResume() {
        refreshNotificationSummary?.invoke(notificationSummary())
    }

    fun onDestroy() {
        setMonitorWeatherState = null
        refreshNotificationSummary = null
    }

    private fun onMonitorWeatherChanged(enabled: Boolean) {
        scope.launch {
            if (enabled && prefs.shouldMonitorWeather) {
                weatherMonitorService.enable()
                RequestRemoveBatteryRestrictionCommand(
                    context,
                    WeatherBatteryRestrictionsAlerter(context, effects, scope)
                ).execute()
            } else {
                weatherMonitorService.disable()
            }
        }
    }

    private fun restartWeatherMonitor() {
        scope.launch {
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
            context,
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
        return options(context.resources.getStringArray(entriesId).toList(), valuesId)
    }

    private fun options(entries: List<CharSequence>, valuesId: Int): List<WeatherOption> {
        val values = context.resources.getStringArray(valuesId)
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
            val readings = WeatherRepo.getInstance(context).getAll()
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
        val exporter = IOFactory().createCsvService(
            context,
            intentResultRetriever
        )
        val repo = WeatherRepo.getInstance(context)
        scope.launch {
            val exported = withContext(Dispatchers.IO) {
                val readings = repo.getAll().sortedByDescending { it.time }
                val csv = WeatherCsvConverter().toCSV(readings)
                exporter.export(csv, "weather-${Instant.now().toEpochMilli()}.csv")
            }
            if (exported) {
                withContext(Dispatchers.Main) {
                    Alerts.toast(context, getString(R.string.weather_exported))
                }
            }
        }
    }

    private fun notificationSummary(): String {
        val isBlocked = Notify.isChannelBlocked(
            context,
            CurrentWeatherAlerter.WEATHER_CHANNEL_ID
        )
        return if (isBlocked) getString(R.string.off) else getString(R.string.on)
    }

    private fun getString(@StringRes id: Int, vararg args: Any): String {
        return context.getString(id, *args)
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
