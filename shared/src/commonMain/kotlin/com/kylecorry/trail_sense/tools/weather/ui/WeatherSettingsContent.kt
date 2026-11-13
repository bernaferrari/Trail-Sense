@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.weather.ui

import kotlinx.coroutines.IO
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString

import com.kylecorry.trail_sense.shared.views.compose.AppHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.kylecorry.trail_sense.shared.io.ExportService
import com.kylecorry.trail_sense.shared.notifications.NotificationChannelControl
import com.kylecorry.trail_sense.shared.permissions.BatteryRestrictionPrompt
import com.kylecorry.trail_sense.shared.permissions.BatteryRestrictionsAlerter
import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.tools.weather.WeatherBroadcasts
import com.kylecorry.trail_sense.tools.weather.infrastructure.persistence.IWeatherRepo
import org.jetbrains.compose.resources.StringArrayResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.preferences.Preferences
import com.kylecorry.sol.math.statistics.Statistics
import com.kylecorry.sol.units.Pressure
import com.kylecorry.sol.units.PressureUnits
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.settings.PreferenceKeys
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsInfoRow
import com.kylecorry.trail_sense.settings.ui.SettingsOption
import com.kylecorry.trail_sense.settings.ui.SettingsPickerRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.settings.ui.optionLabel
import com.kylecorry.trail_sense.settings.ui.pickOption
import com.kylecorry.trail_sense.settings.ui.rememberSettingsOptions
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.Units
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.dialogs.Pickers
import com.kylecorry.trail_sense.shared.navigation.rememberSystemBack
import com.kylecorry.trail_sense.shared.safeRoundToInt
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcastEffect
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolCatalog
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherCsvConverter
import com.kylecorry.trail_sense.tools.weather.infrastructure.WeatherPreferences
import com.kylecorry.trail_sense.tools.weather.infrastructure.commands.ChangeWeatherFrequencyCommand
import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime

/**
 * The weather settings page. [quickActionOptions] are the platform's quick actions; [notificationChannel] is the
 * weather monitor's notification channel and [csvExporter] saves the readings as CSV (each row hides when null).
 */
@Composable
fun WeatherSettingsScreen(
    quickActionOptions: List<SettingsOption>,
    notificationChannel: NotificationChannelControl?,
    csvExporter: ExportService<List<List<String>>>?
) {
    val host = AppHost.current()
    val strings = LocalAppStrings.current
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val dispatcher = rememberSystemBack()
    val viewModel = viewModel {
        WeatherSettingsViewModel(
            prefs = getAppService<UserPreferences>().weather,
            cache = getAppService<Preferences>(),
            monitor = getAppService<ToolCatalog>().getService(WeatherBroadcasts.SERVICE_WEATHER_MONITOR)!!,
            repo = getAppService<IWeatherRepo>(),
            isNotificationBlocked = { notificationChannel?.isBlocked() ?: false },
            flagKeys = WeatherFlag.entries.associateWith { it.key },
            choiceKeys = WeatherChoice.entries.associateWith { it.key }
        )
    }
    val userPrefs = remember { getAppService<UserPreferences>() }
    val formatService = remember { FormatService.getInstance() }

    ToolBroadcastEffect(WeatherBroadcasts.MONITOR_STATE_CHANGED) {
        viewModel.onMonitorStateChanged()
    }

    LifecycleResumeEffect(viewModel) {
        viewModel.refreshNotificationState()
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                WeatherSettingsEvent.MonitorEnabled -> getAppService<BatteryRestrictionPrompt>().request(
                    BatteryRestrictionsAlerter(effects, scope)
                )
            }
        }
    }

    if (isDebug()) {
        LaunchedEffect(Unit) {
            viewModel.loadDebugTimingSummary()
        }
    }

    val pressureHistoryOptions = rememberSettingsOptions(Res.array.pressure_history_entries, Res.array.pressure_history_values)
    val forecastSensitivityOptions = remember(strings) {
        weatherOptions(
            strings,
            sensitivities(strings, formatService, userPrefs.pressureUnits, FORECAST_SENSITIVITIES),
            Res.array.forecast_sensitivity_values
        )
    }
    val stormSensitivityOptions = remember(strings) {
        weatherOptions(
            strings,
            sensitivities(strings, formatService, userPrefs.pressureUnits, STORM_SENSITIVITIES),
            Res.array.storm_alert_sensitivity_values
        )
    }
    val muteStormAlarmSummary = remember(strings) {
        strings.get(
            Res.string.alarm_time_range,
            formatService.formatTime(LocalTime(8, 0), includeSeconds = false, includeMinutes = false),
            formatService.formatTime(LocalTime(20, 0), includeSeconds = false, includeMinutes = false)
        )
    }

    var showUpdateFrequencyPicker by remember { mutableStateOf(false) }
    val updateFrequencyTitle = appString(Res.string.pref_weather_update_frequency_title)

    fun pickChoice(title: String, options: List<SettingsOption>, choice: WeatherChoice) {
        pickOption(title, options, viewModel.choice(choice)) {
            viewModel.changeChoice(choice, it)
        }
    }

    @Composable
    fun FlagRow(
        flag: WeatherFlag,
        label: String,
        position: TrailSenseGroupPosition,
        summary: String? = null
    ) {
        SettingsSwitchRow(
            label = label,
            summary = summary,
            checked = viewModel.flag(flag),
            onCheckedChange = { viewModel.changeFlag(flag, it) },
            position = position
        )
    }

    val monitorWeather = viewModel.flag(WeatherFlag.MonitorWeather)
    val generalTitle = appString(Res.string.general)
    val forecastTitle = appString(Res.string.forecasts_category)
    val notificationTitle = appString(Res.string.settings_notifications_section)
    val stormTitle = appString(Res.string.weather_storm)
    val advancedTitle = appString(Res.string.advanced)
    val notificationChannelTitle =
        appString(Res.string.notifications_channel, appString(Res.string.weather_monitor))

    TrailSensePage(
        title = appString(Res.string.weather),
        onBack = { dispatcher?.invoke() },
        backLabel = appString(Res.string.back)
    ) {
        trailSenseSection("general", title = generalTitle) {
            row { position ->
                SettingsPickerRow(
                    label = appString(Res.string.left_quick_action),
                    selected = optionLabel(quickActionOptions, viewModel.choice(WeatherChoice.LeftQuickAction)),
                    onClick = {
                        pickChoice(
                            strings.get(Res.string.left_quick_action),
                            quickActionOptions,
                            WeatherChoice.LeftQuickAction
                        )
                    },
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = appString(Res.string.right_quick_action),
                    selected = optionLabel(quickActionOptions, viewModel.choice(WeatherChoice.RightQuickAction)),
                    onClick = {
                        pickChoice(
                            strings.get(Res.string.right_quick_action),
                            quickActionOptions,
                            WeatherChoice.RightQuickAction
                        )
                    },
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = appString(Res.string.pref_pressure_history_title),
                    selected = optionLabel(pressureHistoryOptions, viewModel.choice(WeatherChoice.PressureHistory)),
                    onClick = {
                        pickChoice(
                            strings.get(Res.string.pref_pressure_history_title),
                            pressureHistoryOptions,
                            WeatherChoice.PressureHistory
                        )
                    },
                    position = position
                )
            }
        }

        trailSenseSection("forecast", title = forecastTitle) {
            if (viewModel.monitorWeatherAvailable) {
                row { position ->
                    SettingsSwitchRow(
                        label = appString(Res.string.pref_monitor_weather_title),
                        summary = appString(Res.string.pref_monitor_weather_summary),
                        checked = monitorWeather,
                        onCheckedChange = viewModel::changeMonitorWeather,
                        position = position
                    )
                }
            }
            row { position ->
                SettingsPickerRow(
                    label = appString(Res.string.pref_forecast_sensitivity_title),
                    selected = optionLabel(
                        forecastSensitivityOptions,
                        viewModel.choice(WeatherChoice.ForecastSensitivity)
                    ),
                    onClick = {
                        pickChoice(
                            strings.get(Res.string.pref_forecast_sensitivity_title),
                            forecastSensitivityOptions,
                            WeatherChoice.ForecastSensitivity
                        )
                    },
                    position = position
                )
            }
        }

        trailSenseSection("notification", title = notificationTitle) {
            if (notificationChannel != null) {
                row { position ->
                    SettingsPickerRow(
                        label = notificationChannelTitle,
                        selected = appString(if (viewModel.notificationsBlocked) Res.string.off else Res.string.on),
                        onClick = notificationChannel::openSettings,
                        position = position
                    )
                }
            }
            row { position ->
                FlagRow(
                    WeatherFlag.ShowPressureInNotification,
                    appString(Res.string.pref_show_pressure_in_notification_title),
                    position
                )
            }
            row { position ->
                FlagRow(
                    WeatherFlag.ShowTemperatureInNotification,
                    appString(Res.string.pref_show_temperature_in_notification_title),
                    position
                )
            }
            if (monitorWeather) {
                row { position ->
                    FlagRow(
                        WeatherFlag.DailyWeatherNotification,
                        appString(Res.string.pref_daily_weather_notification_title),
                        position
                    )
                }
                if (viewModel.flag(WeatherFlag.DailyWeatherNotification)) {
                    row { position ->
                        SettingsPickerRow(
                            label = appString(Res.string.pref_daily_weather_time_title),
                            selected = formatService.formatTime(viewModel.dailyWeatherTime, false),
                            onClick = {
                                Pickers.time(
                                    host,
                                    userPrefs.use24HourTime,
                                    viewModel.dailyWeatherTime
                                ) { time ->
                                    if (time != null) {
                                        viewModel.changeDailyWeatherTime(time)
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
                    FlagRow(WeatherFlag.StormAlerts, appString(Res.string.storm_alerts), position)
                }
            }
            row { position ->
                SettingsPickerRow(
                    label = appString(Res.string.pref_storm_alert_sensitivity_title),
                    selected = optionLabel(stormSensitivityOptions, viewModel.choice(WeatherChoice.StormSensitivity)),
                    onClick = {
                        pickChoice(
                            strings.get(Res.string.pref_storm_alert_sensitivity_title),
                            stormSensitivityOptions,
                            WeatherChoice.StormSensitivity
                        )
                    },
                    position = position
                )
            }
            if (monitorWeather && viewModel.flag(WeatherFlag.StormAlerts)) {
                row { position ->
                    FlagRow(
                        WeatherFlag.UseAlarmForStormAlert,
                        appString(Res.string.use_alarm_for_storm_alert),
                        position,
                        summary = appString(Res.string.alarm_description)
                    )
                }
                if (viewModel.flag(WeatherFlag.UseAlarmForStormAlert)) {
                    row { position ->
                        FlagRow(
                            WeatherFlag.MuteStormAlarmAtNight,
                            appString(Res.string.mute_alarm_at_night),
                            position,
                            summary = muteStormAlarmSummary
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
                        selected = formatService.formatDuration(viewModel.updateFrequency),
                        onClick = { showUpdateFrequencyPicker = true },
                        position = position
                    )
                }
                row { position ->
                    FlagRow(
                        WeatherFlag.ShowDetailedIcon,
                        appString(Res.string.pref_weather_show_detailed_icon_title),
                        position
                    )
                }
            }
            if (csvExporter != null) {
                row { position ->
                    SettingsActionRow(
                        label = appString(Res.string.pref_export_weather_csv_title),
                        onClick = {
                            scope.launch {
                                val csv = viewModel.exportCsv()
                                val exported = withContext(Dispatchers.IO) {
                                    csvExporter.export(csv, "weather-${Clock.System.now().toEpochMilliseconds()}.csv")
                                }
                                if (exported) {
                                    Alerts.snackbar(host, strings.get(Res.string.weather_exported))
                                }
                            }
                        },
                        position = position
                    )
                }
            }
            if (isDebug()) {
                row { position ->
                    SettingsInfoRow(
                        label = "Timing (Debug)",
                        summary = viewModel.debugTimingSummary,
                        position = position
                    )
                }
            }
        }
    }

    if (showUpdateFrequencyPicker) {
        DurationPickerDialog(
            title = updateFrequencyTitle,
            duration = viewModel.updateFrequency,
            message = appString(Res.string.actual_frequency_disclaimer),
            hint = appString(Res.string.frequency),
            onDurationPicked = {
                if (it != null) {
                    ChangeWeatherFrequencyCommand(host) { frequency ->
                        viewModel.onUpdateFrequencyChanged(frequency)
                    }.execute(it)
                }
                showUpdateFrequencyPicker = false
            },
            onDismiss = { showUpdateFrequencyPicker = false }
        )
    }
}

private val FORECAST_SENSITIVITIES = listOf(
    WeatherPreferences.HPA_FORECAST_LOW,
    WeatherPreferences.HPA_FORECAST_MEDIUM,
    WeatherPreferences.HPA_FORECAST_HIGH
)

private val STORM_SENSITIVITIES = listOf(
    WeatherPreferences.HPA_STORM_LOW,
    WeatherPreferences.HPA_STORM_MEDIUM,
    WeatherPreferences.HPA_STORM_HIGH
)

/** Low / medium / high, each with its pressure change in the user's units. */
private fun sensitivities(
    strings: AppStrings,
    formatService: FormatService,
    units: PressureUnits,
    thresholds: List<Float>
): List<CharSequence> {
    val stringValues = thresholds.map {
        strings.get(
            Res.string.pressure_tendency_format_2,
            formatService.formatPressure(
                Pressure.from(it, PressureUnits.Hpa).convertTo(units),
                Units.getDecimalPlaces(units) + 1,
                false
            )
        )
    }

    return listOf(
        strings.get(Res.string.low_amount, stringValues[0]),
        strings.get(Res.string.medium_amount, stringValues[1]),
        strings.get(Res.string.high_amount, stringValues[2])
    )
}

private fun weatherOptions(
    strings: AppStrings,
    entries: List<CharSequence>,
    values: StringArrayResource
): List<SettingsOption> {
    val values = strings.array(values)
    return values.mapIndexed { index, value ->
        SettingsOption(value, entries.getOrElse(index) { value }.toString())
    }
}

/** The weather switches stored directly under a preference key; some restart the monitor when changed. */
internal enum class WeatherFlag(val key: String, val default: Boolean, val restartsMonitor: Boolean = false) {
    MonitorWeather(PreferenceKeys.monitorWeather, true),
    ShowPressureInNotification(PreferenceKeys.showPressureInNotification, false, restartsMonitor = true),
    ShowTemperatureInNotification(PreferenceKeys.showTemperatureInNotification, true, restartsMonitor = true),
    DailyWeatherNotification(PreferenceKeys.dailyWeatherNotification, true, restartsMonitor = true),
    StormAlerts(PreferenceKeys.sendStormAlert, true),
    UseAlarmForStormAlert(PreferenceKeys.weatherUseAlarmForStormAlert, false),
    MuteStormAlarmAtNight(PreferenceKeys.weatherMuteStormAlarmAtNight, true),
    ShowDetailedIcon(PreferenceKeys.weatherShowDetailedIcon, true)
}

/** The weather pickers stored directly as a string under a preference key. */
internal enum class WeatherChoice(val key: String, val default: String?) {
    /** The quick actions start from the preference's own parsing, so they have no plain default. */
    LeftQuickAction(PreferenceKeys.weatherQuickActionLeft, null),
    RightQuickAction(PreferenceKeys.weatherQuickActionRight, null),
    PressureHistory(PreferenceKeys.pressureHistory, "48"),
    ForecastSensitivity(PreferenceKeys.forecastSensitivity, "medium"),
    StormSensitivity(PreferenceKeys.stormAlertSensitivity, "medium")
}

internal sealed interface WeatherSettingsEvent {
    /** The weather monitor was just turned on, so the battery restriction should be offered for removal. */
    data object MonitorEnabled : WeatherSettingsEvent
}

/**
 * The weather settings: the stored switches and choices, the monitor (and whether it is available), the daily
 * forecast time, the update frequency, the notification channel's state, and the CSV export's data.
 */
internal class WeatherSettingsViewModel(
    private val prefs: WeatherPreferences,
    private val cache: Preferences,
    private val monitor: ToolService,
    private val repo: IWeatherRepo,
    private val isNotificationBlocked: () -> Boolean,
    private val flagKeys: Map<WeatherFlag, String>,
    private val choiceKeys: Map<WeatherChoice, String>
) : ViewModel() {

    private val flags = mutableStateMapOf<WeatherFlag, Boolean>().apply {
        WeatherFlag.entries.forEach { put(it, cache.getBoolean(flagKeys.getValue(it)) ?: it.default) }
    }
    private val choices = mutableStateMapOf<WeatherChoice, String>().apply {
        WeatherChoice.entries.forEach {
            val value = when (it) {
                WeatherChoice.LeftQuickAction -> prefs.leftButton.toString()
                WeatherChoice.RightQuickAction -> prefs.rightButton.toString()
                else -> cache.getString(choiceKeys.getValue(it)) ?: it.default.orEmpty()
            }
            put(it, value)
        }
    }

    private val eventChannel = Channel<WeatherSettingsEvent>(Channel.BUFFERED)
    val events: Flow<WeatherSettingsEvent> = eventChannel.receiveAsFlow()

    var monitorWeatherAvailable by mutableStateOf(!monitor.isBlocked())
        private set
    var notificationsBlocked by mutableStateOf(isNotificationBlocked())
        private set
    var dailyWeatherTime by mutableStateOf(prefs.dailyForecastTime)
        private set
    var updateFrequency by mutableStateOf(prefs.weatherUpdateFrequency)
        private set
    var debugTimingSummary by mutableStateOf<String?>(null)
        private set

    fun flag(flag: WeatherFlag): Boolean = flags[flag] ?: flag.default

    fun choice(choice: WeatherChoice): String = choices[choice].orEmpty()

    fun changeFlag(flag: WeatherFlag, value: Boolean) {
        flags[flag] = value
        cache.putBoolean(flagKeys.getValue(flag), value)
        if (flag.restartsMonitor) {
            restartMonitor()
        }
    }

    fun changeChoice(choice: WeatherChoice, value: String) {
        choices[choice] = value
        cache.putString(choiceKeys.getValue(choice), value)
    }

    fun changeMonitorWeather(enabled: Boolean) {
        flags[WeatherFlag.MonitorWeather] = enabled
        prefs.shouldMonitorWeather = enabled
        viewModelScope.launch {
            if (enabled && prefs.shouldMonitorWeather) {
                monitor.enable()
                eventChannel.send(WeatherSettingsEvent.MonitorEnabled)
            } else {
                monitor.disable()
            }
        }
    }

    fun changeDailyWeatherTime(time: LocalTime) {
        dailyWeatherTime = time
        prefs.dailyForecastTime = time
        restartMonitor()
    }

    fun onUpdateFrequencyChanged(frequency: Duration) {
        updateFrequency = frequency
    }

    /** The monitor was turned on or off (or blocked) elsewhere. */
    fun onMonitorStateChanged() {
        flags[WeatherFlag.MonitorWeather] = monitor.isEnabled()
        monitorWeatherAvailable = !monitor.isBlocked()
    }

    fun refreshNotificationState() {
        notificationsBlocked = isNotificationBlocked()
    }

    /** Every reading, newest first, as CSV rows. */
    suspend fun exportCsv(): List<List<String>> = withContext(Dispatchers.IO) {
        val readings = repo.getAll().sortedByDescending { it.time }
        WeatherCsvConverter().toCSV(readings)
    }

    suspend fun loadDebugTimingSummary() {
        debugTimingSummary = withContext(Dispatchers.Default) {
            val readings = repo.getAll()
                .zipWithNext { a, b -> (b.time - a.time).inWholeSeconds / 60f }

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

    private fun restartMonitor() {
        viewModelScope.launch {
            monitor.restart()
        }
    }
}
