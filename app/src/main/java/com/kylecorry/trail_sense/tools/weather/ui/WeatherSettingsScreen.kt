@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.weather.ui

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.kylecorry.andromeda.core.system.IntentResultRetriever
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import kotlinx.coroutines.launch

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.trail_sense.settings.ui.SettingsOption
import com.kylecorry.trail_sense.settings.ui.optionLabel
import com.kylecorry.trail_sense.settings.ui.pickOption
import com.kylecorry.trail_sense.settings.ui.rememberSettingsOptions
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseGroupPosition
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
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
    val appContext = context.applicationContext
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    // MainActivity is the app's IntentResultRetriever, which the CSV export's file picker needs.
    val intentResultRetriever = LocalActivity.current as IntentResultRetriever
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val viewModel = viewModel {
        WeatherSettingsViewModel(
            prefs = UserPreferences(appContext).weather,
            cache = PreferencesSubsystem.getInstance(appContext).preferences,
            monitor = Tools.getService(appContext, WeatherToolRegistration.SERVICE_WEATHER_MONITOR)!!,
            repo = WeatherRepo.getInstance(appContext),
            isNotificationBlocked = {
                Notify.isChannelBlocked(appContext, CurrentWeatherAlerter.WEATHER_CHANNEL_ID)
            },
            flagKeys = WeatherFlag.entries.associateWith { appContext.getString(it.key) },
            choiceKeys = WeatherChoice.entries.associateWith { appContext.getString(it.key) }
        )
    }
    val userPrefs = remember(appContext) { UserPreferences(appContext) }
    val formatService = remember(appContext) { FormatService.getInstance(appContext) }

    ToolBroadcastEffect(WeatherToolRegistration.BROADCAST_WEATHER_MONITOR_STATE_CHANGED) {
        viewModel.onMonitorStateChanged()
    }

    LifecycleResumeEffect(viewModel) {
        viewModel.refreshNotificationState()
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect {
            when (it) {
                WeatherSettingsEvent.MonitorEnabled -> RequestRemoveBatteryRestrictionCommand(
                    context,
                    WeatherBatteryRestrictionsAlerter(context, effects, scope)
                ).execute()
            }
        }
    }

    if (isDebug()) {
        LaunchedEffect(Unit) {
            viewModel.loadDebugTimingSummary()
        }
    }

    val quickActionOptions = remember(context) {
        Tools.getQuickActions(context).map { SettingsOption(it.id.toString(), it.name) }
    }
    val pressureHistoryOptions = rememberSettingsOptions(R.array.pressure_history_entries, R.array.pressure_history_values)
    val forecastSensitivityOptions = remember(context) {
        context.weatherOptions(
            sensitivities(context, formatService, userPrefs.pressureUnits, FORECAST_SENSITIVITIES),
            R.array.forecast_sensitivity_values
        )
    }
    val stormSensitivityOptions = remember(context) {
        context.weatherOptions(
            sensitivities(context, formatService, userPrefs.pressureUnits, STORM_SENSITIVITIES),
            R.array.storm_alert_sensitivity_values
        )
    }
    val muteStormAlarmSummary = remember(context) {
        context.getString(
            R.string.alarm_time_range,
            formatService.formatTime(LocalTime.of(8, 0), includeSeconds = false, includeMinutes = false),
            formatService.formatTime(LocalTime.of(20, 0), includeSeconds = false, includeMinutes = false)
        )
    }

    var showUpdateFrequencyPicker by remember { mutableStateOf(false) }
    val updateFrequencyTitle = stringResource(R.string.pref_weather_update_frequency_title)

    fun pickChoice(title: String, options: List<SettingsOption>, choice: WeatherChoice) {
        context.pickOption(title, options, viewModel.choice(choice)) {
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
    val generalTitle = stringResource(R.string.general)
    val forecastTitle = stringResource(R.string.forecasts_category)
    val notificationTitle = stringResource(R.string.settings_notifications_section)
    val stormTitle = stringResource(R.string.weather_storm)
    val advancedTitle = stringResource(R.string.advanced)
    val notificationChannelTitle =
        stringResource(R.string.notifications_channel, stringResource(R.string.weather_monitor))

    TrailSensePage(
        title = stringResource(R.string.weather),
        onBack = { dispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back)
    ) {
        trailSenseSection("general", title = generalTitle) {
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.left_quick_action),
                    selected = optionLabel(quickActionOptions, viewModel.choice(WeatherChoice.LeftQuickAction)),
                    onClick = {
                        pickChoice(
                            context.getString(R.string.left_quick_action),
                            quickActionOptions,
                            WeatherChoice.LeftQuickAction
                        )
                    },
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.right_quick_action),
                    selected = optionLabel(quickActionOptions, viewModel.choice(WeatherChoice.RightQuickAction)),
                    onClick = {
                        pickChoice(
                            context.getString(R.string.right_quick_action),
                            quickActionOptions,
                            WeatherChoice.RightQuickAction
                        )
                    },
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.pref_pressure_history_title),
                    selected = optionLabel(pressureHistoryOptions, viewModel.choice(WeatherChoice.PressureHistory)),
                    onClick = {
                        pickChoice(
                            context.getString(R.string.pref_pressure_history_title),
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
                        label = stringResource(R.string.pref_monitor_weather_title),
                        summary = stringResource(R.string.pref_monitor_weather_summary),
                        checked = monitorWeather,
                        onCheckedChange = viewModel::changeMonitorWeather,
                        position = position
                    )
                }
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.pref_forecast_sensitivity_title),
                    selected = optionLabel(
                        forecastSensitivityOptions,
                        viewModel.choice(WeatherChoice.ForecastSensitivity)
                    ),
                    onClick = {
                        pickChoice(
                            context.getString(R.string.pref_forecast_sensitivity_title),
                            forecastSensitivityOptions,
                            WeatherChoice.ForecastSensitivity
                        )
                    },
                    position = position
                )
            }
        }

        trailSenseSection("notification", title = notificationTitle) {
            row { position ->
                SettingsPickerRow(
                    label = notificationChannelTitle,
                    selected = stringResource(if (viewModel.notificationsBlocked) R.string.off else R.string.on),
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
                FlagRow(
                    WeatherFlag.ShowPressureInNotification,
                    stringResource(R.string.pref_show_pressure_in_notification_title),
                    position
                )
            }
            row { position ->
                FlagRow(
                    WeatherFlag.ShowTemperatureInNotification,
                    stringResource(R.string.pref_show_temperature_in_notification_title),
                    position
                )
            }
            if (monitorWeather) {
                row { position ->
                    FlagRow(
                        WeatherFlag.DailyWeatherNotification,
                        stringResource(R.string.pref_daily_weather_notification_title),
                        position
                    )
                }
                if (viewModel.flag(WeatherFlag.DailyWeatherNotification)) {
                    row { position ->
                        SettingsPickerRow(
                            label = stringResource(R.string.pref_daily_weather_time_title),
                            selected = formatService.formatTime(viewModel.dailyWeatherTime, false),
                            onClick = {
                                Pickers.time(
                                    context,
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
                    FlagRow(WeatherFlag.StormAlerts, stringResource(R.string.storm_alerts), position)
                }
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.pref_storm_alert_sensitivity_title),
                    selected = optionLabel(stormSensitivityOptions, viewModel.choice(WeatherChoice.StormSensitivity)),
                    onClick = {
                        pickChoice(
                            context.getString(R.string.pref_storm_alert_sensitivity_title),
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
                        stringResource(R.string.use_alarm_for_storm_alert),
                        position,
                        summary = stringResource(R.string.alarm_description)
                    )
                }
                if (viewModel.flag(WeatherFlag.UseAlarmForStormAlert)) {
                    row { position ->
                        FlagRow(
                            WeatherFlag.MuteStormAlarmAtNight,
                            stringResource(R.string.mute_alarm_at_night),
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
                        stringResource(R.string.pref_weather_show_detailed_icon_title),
                        position
                    )
                }
            }
            row { position ->
                SettingsActionRow(
                    label = stringResource(R.string.pref_export_weather_csv_title),
                    onClick = {
                        val exporter = IOFactory().createCsvService(context, intentResultRetriever)
                        scope.launch {
                            val csv = viewModel.exportCsv()
                            val exported = withContext(Dispatchers.IO) {
                                exporter.export(csv, "weather-${Instant.now().toEpochMilli()}.csv")
                            }
                            if (exported) {
                                Alerts.toast(context, context.getString(R.string.weather_exported))
                            }
                        }
                    },
                    position = position
                )
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
            message = stringResource(R.string.actual_frequency_disclaimer),
            hint = stringResource(R.string.frequency),
            onDurationPicked = {
                if (it != null) {
                    ChangeWeatherFrequencyCommand(context) { frequency ->
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
    context: android.content.Context,
    formatService: FormatService,
    units: PressureUnits,
    thresholds: List<Float>
): List<CharSequence> {
    val stringValues = thresholds.map {
        context.getString(
            R.string.pressure_tendency_format_2,
            formatService.formatPressure(
                Pressure.from(it, PressureUnits.Hpa).convertTo(units),
                Units.getDecimalPlaces(units) + 1,
                false
            )
        )
    }

    return listOf(
        context.getString(R.string.low_amount, stringValues[0]),
        context.getString(R.string.medium_amount, stringValues[1]),
        context.getString(R.string.high_amount, stringValues[2])
    )
}

private fun android.content.Context.weatherOptions(
    entries: List<CharSequence>,
    valuesId: Int
): List<SettingsOption> {
    val values = resources.getStringArray(valuesId)
    return values.mapIndexed { index, value ->
        SettingsOption(value, entries.getOrElse(index) { value }.toString())
    }
}

/** The weather switches stored directly under a preference key; some restart the monitor when changed. */
internal enum class WeatherFlag(@StringRes val key: Int, val default: Boolean, val restartsMonitor: Boolean = false) {
    MonitorWeather(R.string.pref_monitor_weather, true),
    ShowPressureInNotification(R.string.pref_show_pressure_in_notification, false, restartsMonitor = true),
    ShowTemperatureInNotification(R.string.pref_show_temperature_in_notification, true, restartsMonitor = true),
    DailyWeatherNotification(R.string.pref_daily_weather_notification, true, restartsMonitor = true),
    StormAlerts(R.string.pref_send_storm_alert, true),
    UseAlarmForStormAlert(R.string.pref_weather_use_alarm_for_storm_alert, false),
    MuteStormAlarmAtNight(R.string.pref_weather_mute_storm_alarm_at_night, true),
    ShowDetailedIcon(R.string.pref_weather_show_detailed_icon, true)
}

/** The weather pickers stored directly as a string under a preference key. */
internal enum class WeatherChoice(@StringRes val key: Int, val default: String?) {
    /** The quick actions start from the preference's own parsing, so they have no plain default. */
    LeftQuickAction(R.string.pref_weather_quick_action_left, null),
    RightQuickAction(R.string.pref_weather_quick_action_right, null),
    PressureHistory(R.string.pref_pressure_history, "48"),
    ForecastSensitivity(R.string.pref_forecast_sensitivity, "medium"),
    StormSensitivity(R.string.pref_storm_alert_sensitivity, "medium")
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
    private val cache: IPreferences,
    private val monitor: ToolService,
    private val repo: WeatherRepo,
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

    private fun restartMonitor() {
        viewModelScope.launch {
            monitor.restart()
        }
    }
}
