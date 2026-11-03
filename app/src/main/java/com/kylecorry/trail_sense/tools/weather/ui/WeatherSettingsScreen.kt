@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.weather.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.coroutines.BackgroundMinimumState
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
import com.kylecorry.trail_sense.shared.permissions.RemoveBatteryRestrictionsAlerter
import com.kylecorry.trail_sense.shared.permissions.RequestRemoveBatteryRestrictionCommand
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize
import androidx.compose.ui.unit.dp
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
fun WeatherSettingsToolScreen(host: TrailSenseDestinationHost) {
    val controller = remember(host) { WeatherSettingsController(host) }
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

private class WeatherSettingsController(private val host: TrailSenseDestinationHost) {

    private val cache by lazy { PreferencesSubsystem.getInstance(host.requireContext()).preferences }
    private val userPrefs by lazy { UserPreferences(host.requireContext()) }
    private val prefs by lazy { userPrefs.weather }
    private val formatService by lazy { FormatService.getInstance(host.requireContext()) }

    private val weatherMonitorService by lazy {
        Tools.getService(
            host.requireContext(),
            WeatherToolRegistration.SERVICE_WEATHER_MONITOR
        )!!
    }

    private var setMonitorWeatherState: ((Boolean, Boolean) -> Unit)? = null
    private var refreshNotificationSummary: ((String) -> Unit)? = null

    @Composable
    fun Content() {
                    val quickActionOptions = remember {
                        Tools.getQuickActions(host.requireContext()).map {
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
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = TrailSenseSize.maximumContentWidth),
                                contentPadding = PaddingValues(
                                    start = TrailSenseSpacing.screen,
                                    end = TrailSenseSpacing.screen,
                                    top = TrailSenseSpacing.lg,
                                    bottom = TrailSenseSpacing.xxxl
                                ),
                                verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.lg)
                            ) {
                            item("title") {
                                Text(
                                    text = stringResource(R.string.weather),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(
                                        top = TrailSenseSpacing.sm,
                                        bottom = TrailSenseSpacing.xs
                                    )
                                )
                            }
                            item("general_section") {
                                WeatherSettingsSection(title = stringResource(R.string.general)) {
                                    WeatherSettingsClickRow(
                                        title = stringResource(R.string.left_quick_action),
                                        subtitle = optionLabel(quickActionOptions, leftQuickAction),
                                        icon = painterResource(R.drawable.ic_weather),
                                        onClick = {
                                            pickOption(
                                                getString(R.string.left_quick_action),
                                                quickActionOptions,
                                                leftQuickAction
                                            ) {
                                                leftQuickAction = it
                                                putString(R.string.pref_weather_quick_action_left, it)
                                            }
                                        }
                                    )
                                    WeatherSettingsCardDivider()
                                    WeatherSettingsClickRow(
                                        title = stringResource(R.string.right_quick_action),
                                        subtitle = optionLabel(quickActionOptions, rightQuickAction),
                                        onClick = {
                                            pickOption(
                                                getString(R.string.right_quick_action),
                                                quickActionOptions,
                                                rightQuickAction
                                            ) {
                                                rightQuickAction = it
                                                putString(R.string.pref_weather_quick_action_right, it)
                                            }
                                        }
                                    )
                                    WeatherSettingsCardDivider()
                                    WeatherSettingsClickRow(
                                        title = stringResource(R.string.pref_pressure_history_title),
                                        subtitle = optionLabel(pressureHistoryOptions, pressureHistory),
                                        icon = painterResource(R.drawable.ic_barometer),
                                        onClick = {
                                            pickOption(
                                                getString(R.string.pref_pressure_history_title),
                                                pressureHistoryOptions,
                                                pressureHistory
                                            ) {
                                                pressureHistory = it
                                                putString(R.string.pref_pressure_history, it)
                                            }
                                        }
                                    )
                                }
                            }

                            item("forecast_section") {
                                WeatherSettingsSection(
                                    title = stringResource(R.string.forecasts_category)
                                ) {
                                    WeatherSettingsSwitchRow(
                                        label = stringResource(R.string.pref_monitor_weather_title),
                                        summary = stringResource(R.string.pref_monitor_weather_summary),
                                        icon = painterResource(R.drawable.ic_weather),
                                        checked = monitorWeather,
                                        enabled = monitorWeatherEnabled,
                                        onCheckedChange = {
                                            monitorWeather = it
                                            prefs.shouldMonitorWeather = it
                                            onMonitorWeatherChanged(it)
                                        }
                                    )
                                    WeatherSettingsCardDivider()
                                    WeatherSettingsClickRow(
                                        title = stringResource(R.string.pref_forecast_sensitivity_title),
                                        subtitle = optionLabel(
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
                                        }
                                    )
                                    WeatherSettingsCardDivider()
                                    WeatherSettingsClickRow(
                                        title = getString(
                                            R.string.notifications_channel,
                                            getString(R.string.weather_monitor)
                                        ),
                                        subtitle = notificationSummary,
                                        icon = painterResource(R.drawable.ic_notification),
                                        onClick = {
                                            host.startActivity(
                                                Intents.notificationSettings(
                                                    host.requireContext(),
                                                    CurrentWeatherAlerter.WEATHER_CHANNEL_ID
                                                )
                                            )
                                        }
                                    )
                                    WeatherSettingsCardDivider()
                                    WeatherSettingsSwitchRow(
                                        label = stringResource(R.string.pref_show_pressure_in_notification_title),
                                        checked = showPressureInNotification,
                                        onCheckedChange = {
                                            showPressureInNotification = it
                                            putBoolean(
                                                R.string.pref_show_pressure_in_notification,
                                                it
                                            )
                                            restartWeatherMonitor()
                                        }
                                    )
                                    WeatherSettingsCardDivider()
                                    WeatherSettingsSwitchRow(
                                        label = stringResource(R.string.pref_show_temperature_in_notification_title),
                                        checked = showTemperatureInNotification,
                                        onCheckedChange = {
                                            showTemperatureInNotification = it
                                            putBoolean(
                                                R.string.pref_show_temperature_in_notification,
                                                it
                                            )
                                            restartWeatherMonitor()
                                        }
                                    )
                                    WeatherSettingsCardDivider()
                                    WeatherSettingsSwitchRow(
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
                                        }
                                    )
                                    WeatherSettingsCardDivider()
                                    WeatherSettingsClickRow(
                                        title = stringResource(R.string.pref_daily_weather_time_title),
                                        subtitle = formatService.formatTime(dailyWeatherTime, false),
                                        enabled = dailyWeatherNotification,
                                        onClick = {
                                            Pickers.time(
                                                host.requireContext(),
                                                userPrefs.use24HourTime,
                                                dailyWeatherTime
                                            ) { time ->
                                                if (time != null) {
                                                    dailyWeatherTime = time
                                                    prefs.dailyForecastTime = time
                                                    restartWeatherMonitor()
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            item("storm_section") {
                                WeatherSettingsSection(
                                    title = stringResource(R.string.weather_storm)
                                ) {
                                    WeatherSettingsSwitchRow(
                                        label = stringResource(R.string.storm_alerts),
                                        icon = painterResource(R.drawable.storm),
                                        checked = stormAlerts,
                                        enabled = monitorWeather,
                                        onCheckedChange = {
                                            stormAlerts = it
                                            putBoolean(R.string.pref_send_storm_alert, it)
                                        }
                                    )
                                    WeatherSettingsCardDivider()
                                    WeatherSettingsClickRow(
                                        title = stringResource(R.string.pref_storm_alert_sensitivity_title),
                                        subtitle = optionLabel(stormSensitivityOptions, stormSensitivity),
                                        onClick = {
                                            pickOption(
                                                getString(R.string.pref_storm_alert_sensitivity_title),
                                                stormSensitivityOptions,
                                                stormSensitivity
                                            ) {
                                                stormSensitivity = it
                                                putString(R.string.pref_storm_alert_sensitivity, it)
                                            }
                                        }
                                    )
                                    WeatherSettingsCardDivider()
                                    WeatherSettingsSwitchRow(
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
                                        }
                                    )
                                    WeatherSettingsCardDivider()
                                    WeatherSettingsSwitchRow(
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
                                        }
                                    )
                                }
                            }

                            item("advanced_section") {
                                WeatherSettingsSection(title = stringResource(R.string.advanced)) {
                                    WeatherSettingsClickRow(
                                        title = updateFrequencyTitle,
                                        subtitle = formatService.formatDuration(updateFrequency),
                                        icon = painterResource(R.drawable.ic_alert),
                                        enabled = monitorWeather,
                                        onClick = {
                                            showUpdateFrequencyPicker = true
                                        }
                                    )
                                    WeatherSettingsCardDivider()
                                    WeatherSettingsSwitchRow(
                                        label = stringResource(R.string.pref_weather_show_detailed_icon_title),
                                        checked = showDetailedIcon,
                                        enabled = monitorWeather,
                                        onCheckedChange = {
                                            showDetailedIcon = it
                                            putBoolean(R.string.pref_weather_show_detailed_icon, it)
                                        }
                                    )
                                    WeatherSettingsCardDivider()
                                    WeatherSettingsClickRow(
                                        title = stringResource(R.string.pref_export_weather_csv_title),
                                        icon = painterResource(R.drawable.ic_import_export),
                                        onClick = ::exportWeatherData
                                    )
                                    if (isDebug()) {
                                        WeatherSettingsCardDivider()
                                        WeatherSettingsInfoRow(
                                            title = "Timing (Debug)",
                                            subtitle = debugTimingSummary
                                        )
                                    }
                                }
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
                                    ChangeWeatherFrequencyCommand(host.requireContext()) { frequency ->
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

    fun onCreate() {
        Tools.observe(host, WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_STATE_CHANGED) {
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
        host.inBackground {
            if (enabled && prefs.shouldMonitorWeather) {
                weatherMonitorService.enable()
                RequestRemoveBatteryRestrictionCommand(
                    host.requireContext(),
                    RemoveBatteryRestrictionsAlerter(host)
                ).execute()
            } else {
                weatherMonitorService.disable()
            }
        }
    }

    private fun restartWeatherMonitor() {
        host.inBackground {
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

    private fun options(entriesId: Int, valuesId: Int): List<WeatherOption> {
        return options(host.getResources().getStringArray(entriesId).toList(), valuesId)
    }

    private fun options(entries: List<CharSequence>, valuesId: Int): List<WeatherOption> {
        val values = host.getResources().getStringArray(valuesId)
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
            val readings = WeatherRepo.getInstance(host.requireContext()).getAll()
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
            host.requireContext(),
            host.intentResultRetriever
        )
        val repo = WeatherRepo.getInstance(host.requireContext())
        host.inBackground(BackgroundMinimumState.Created) {
            val exported = withContext(Dispatchers.IO) {
                val readings = repo.getAll().sortedByDescending { it.time }
                val csv = WeatherCsvConverter().toCSV(readings)
                exporter.export(csv, "weather-${Instant.now().toEpochMilli()}.csv")
            }
            if (exported) {
                withContext(Dispatchers.Main) {
                    Alerts.toast(host.requireContext(), getString(R.string.weather_exported))
                }
            }
        }
    }

    private fun notificationSummary(): String {
        val isBlocked = Notify.isChannelBlocked(
            host.requireContext(),
            CurrentWeatherAlerter.WEATHER_CHANNEL_ID
        )
        return if (isBlocked) getString(R.string.off) else getString(R.string.on)
    }

    private fun getString(@StringRes id: Int, vararg args: Any): String {
        return host.getString(id, *args)
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

@Composable
private fun WeatherSettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(
                start = TrailSenseSpacing.md,
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
private fun WeatherSettingsCardDivider() {
    Spacer(modifier = Modifier.height(0.dp))
}

@Composable
private fun WeatherSettingsIconChip(icon: Painter) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.size(TrailSenseSize.leadingIconContainer)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(TrailSenseSize.leadingIcon)
            )
        }
    }
}

@Composable
private fun WeatherSettingsClickRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: Painter? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TrailSenseShape.groupInner),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            WeatherSettingsIconChip(icon)
            Spacer(Modifier.width(TrailSenseSpacing.lg))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(TrailSenseSpacing.md))
        if (trailing != null) {
            Box(modifier = Modifier.size(TrailSenseSize.minimumTouchTarget), contentAlignment = Alignment.Center) {
                trailing()
            }
        } else {
            Box(modifier = Modifier.size(TrailSenseSize.minimumTouchTarget), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        }
    }
}

@Composable
private fun WeatherSettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    icon: Painter? = null,
    enabled: Boolean = true
) {
    val containerColor = if (checked && enabled) {
        lerp(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.primaryContainer, 0.35f)
    } else MaterialTheme.colorScheme.surfaceContainerLow
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TrailSenseShape.groupInner),
        color = containerColor
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            WeatherSettingsIconChip(icon)
            Spacer(Modifier.width(TrailSenseSpacing.lg))
        }
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
            onCheckedChange = null,
            enabled = enabled
        )
        }
    }
}

@Composable
private fun WeatherSettingsInfoRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(TrailSenseShape.groupInner),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(horizontal = TrailSenseSpacing.lg, vertical = TrailSenseSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(TrailSenseSpacing.xxs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    }
}
