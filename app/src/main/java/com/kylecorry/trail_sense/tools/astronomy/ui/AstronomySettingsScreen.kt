@file:Suppress("FunctionNaming")

package com.kylecorry.trail_sense.tools.astronomy.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kylecorry.andromeda.preferences.IPreferences
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsOption
import com.kylecorry.trail_sense.settings.ui.SettingsPickerRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.settings.ui.optionLabel
import com.kylecorry.trail_sense.settings.ui.pickOption
import com.kylecorry.trail_sense.settings.ui.rememberSettingsOptions
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.extensions.asLiveData
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.views.compose.LocalScreenEffects
import com.kylecorry.trail_sense.shared.views.compose.TrailSensePage
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.astronomy.AstronomyToolRegistration
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.AstronomyPreferences
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunriseAlarmReceiver
import com.kylecorry.trail_sense.tools.astronomy.infrastructure.receivers.SunsetAlarmReceiver
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolBroadcastEffect
import com.kylecorry.trail_sense.tools.tools.infrastructure.ToolService
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import kotlinx.coroutines.launch
import java.time.Duration

@Composable
fun AstronomySettingsToolScreen() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val effects = LocalScreenEffects.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val viewModel = viewModel {
        AstronomySettingsViewModel(
            prefs = UserPreferences(appContext).astronomy,
            cache = PreferencesSubsystem.getInstance(appContext).preferences,
            sunsetService = Tools.getService(appContext, AstronomyToolRegistration.SERVICE_SUNSET_ALERTS)!!,
            sunriseService = Tools.getService(appContext, AstronomyToolRegistration.SERVICE_SUNRISE_ALERTS)!!,
            keys = AstronomySettingsKeys(
                sunTimeMode = appContext.getString(R.string.pref_sun_time_mode),
                centerSunAndMoon = appContext.getString(R.string.pref_center_sun_and_moon),
                quickActionLeft = appContext.getString(R.string.pref_astronomy_quick_action_left),
                quickActionRight = appContext.getString(R.string.pref_astronomy_quick_action_right),
                startCameraIn3D = appContext.getString(R.string.pref_start_camera_in_3d_view),
                sunsetAlertTime = appContext.getString(R.string.pref_sunset_alert_time),
                sunriseAlertTime = appContext.getString(R.string.pref_sunrise_alert_time),
                useAlarmForSunset = appContext.getString(R.string.pref_astronomy_use_alarm_for_sunset_alert),
                useAlarmForSunrise = appContext.getString(R.string.pref_astronomy_use_alarm_for_sunrise_alert)
            )
        )
    }

    // A changed alert time restarts its alerts while the page is started, as before.
    DisposableEffect(lifecycleOwner, viewModel) {
        val changes = viewModel.cache.onChange.asLiveData()
        val observer = Observer<String> { viewModel.onPreferenceChanged(it) }
        changes.observe(lifecycleOwner, observer)
        onDispose { changes.removeObserver(observer) }
    }

    // The alerts can be turned on or off elsewhere (the quick action); the switches follow while the page is resumed.
    ToolBroadcastEffect(AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_ENABLED) { viewModel.reloadAlerts() }
    ToolBroadcastEffect(AstronomyToolRegistration.BROADCAST_SUNSET_ALERTS_DISABLED) { viewModel.reloadAlerts() }
    ToolBroadcastEffect(AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_ENABLED) { viewModel.reloadAlerts() }
    ToolBroadcastEffect(AstronomyToolRegistration.BROADCAST_SUNRISE_ALERTS_DISABLED) { viewModel.reloadAlerts() }

    val formatter = remember(appContext) { FormatService.getInstance(appContext) }
    val sunTimeOptions = rememberSettingsOptions(R.array.sun_times_entries, R.array.sun_times_values)
    val quickActionOptions = remember(context) {
        Tools.getQuickActions(context).map { SettingsOption(it.id.toString(), it.name) }
    }
    val sunsetTimeOptions = remember(formatter) {
        timeOptions(formatter, listOf(30L, 60L, 90L, 120L, 150L, 180L))
    }
    val sunriseTimeOptions = remember(formatter) {
        timeOptions(formatter, listOf(0L, 15L, 30L, 45L, 60L, 90L, 120L))
    }
    val showStartCameraIn3D = remember(context) {
        Tools.isToolAvailable(context, Tools.AUGMENTED_REALITY)
    }

    val generalTitle = stringResource(R.string.general)
    val sunsetTitle = stringResource(R.string.sunset)
    val sunriseTitle = stringResource(R.string.sunrise)
    val eventsTitle = stringResource(R.string.alerts)

    TrailSensePage(
        title = stringResource(R.string.astronomy),
        onBack = { backDispatcher?.onBackPressed() },
        backLabel = stringResource(R.string.back)
    ) {
        trailSenseSection("general", title = generalTitle) {
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.pref_sun_time_mode_title),
                    selected = optionLabel(sunTimeOptions, viewModel.sunTimeMode),
                    onClick = {
                        context.pickOption(
                            context.getString(R.string.pref_sun_time_mode_title),
                            sunTimeOptions,
                            viewModel.sunTimeMode,
                            viewModel::changeSunTimeMode
                        )
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_center_sun_and_moon_title),
                    checked = viewModel.centerSunAndMoon,
                    onCheckedChange = viewModel::changeCenterSunAndMoon,
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_show_astronomy_bands_title),
                    checked = viewModel.showAstronomyBands,
                    onCheckedChange = viewModel::changeShowAstronomyBands,
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.left_quick_action),
                    selected = optionLabel(quickActionOptions, viewModel.leftQuickAction),
                    onClick = {
                        context.pickOption(
                            context.getString(R.string.left_quick_action),
                            quickActionOptions,
                            viewModel.leftQuickAction,
                            viewModel::changeLeftQuickAction
                        )
                    },
                    position = position
                )
            }
            row { position ->
                SettingsPickerRow(
                    label = stringResource(R.string.right_quick_action),
                    selected = optionLabel(quickActionOptions, viewModel.rightQuickAction),
                    onClick = {
                        context.pickOption(
                            context.getString(R.string.right_quick_action),
                            quickActionOptions,
                            viewModel.rightQuickAction,
                            viewModel::changeRightQuickAction
                        )
                    },
                    position = position
                )
            }
            if (showStartCameraIn3D) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_start_camera_in_3d_view_title),
                        summary = stringResource(R.string.pref_start_camera_in_3d_view_summary),
                        checked = viewModel.startCameraIn3D,
                        onCheckedChange = viewModel::changeStartCameraIn3D,
                        position = position
                    )
                }
            }
        }

        trailSenseSection("sunset", title = sunsetTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.sunset_alerts),
                    checked = viewModel.sunsetAlerts,
                    onCheckedChange = {
                        viewModel.changeSunsetAlerts(it)
                        if (it) {
                            // Turning alerts on may ask for the alarm or notification permission.
                            scope.launch { SunsetAlarmReceiver.enable(context, effects, scope) }
                        }
                    },
                    position = position
                )
            }
            if (viewModel.sunsetAlerts) {
                row { position ->
                    SettingsPickerRow(
                        label = stringResource(R.string.pref_sunset_alert_time_title),
                        selected = optionLabel(sunsetTimeOptions, viewModel.sunsetAlertTime),
                        onClick = {
                            context.pickOption(
                                context.getString(R.string.pref_sunset_alert_time_title),
                                sunsetTimeOptions,
                                viewModel.sunsetAlertTime,
                                viewModel::changeSunsetAlertTime
                            )
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.use_alarm_for_sunset_alert),
                        summary = stringResource(R.string.alarm_description),
                        checked = viewModel.useAlarmForSunset,
                        onCheckedChange = viewModel::changeUseAlarmForSunset,
                        position = position
                    )
                }
            }
        }

        trailSenseSection("sunrise", title = sunriseTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.sunrise_alerts),
                    checked = viewModel.sunriseAlerts,
                    onCheckedChange = {
                        viewModel.changeSunriseAlerts(it)
                        if (it) {
                            scope.launch { SunriseAlarmReceiver.enable(context, effects, scope) }
                        }
                    },
                    position = position
                )
            }
            if (viewModel.sunriseAlerts) {
                row { position ->
                    SettingsPickerRow(
                        label = stringResource(R.string.pref_sunrise_alert_time_title),
                        selected = optionLabel(sunriseTimeOptions, viewModel.sunriseAlertTime),
                        onClick = {
                            context.pickOption(
                                context.getString(R.string.pref_sunrise_alert_time_title),
                                sunriseTimeOptions,
                                viewModel.sunriseAlertTime,
                                viewModel::changeSunriseAlertTime
                            )
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.use_alarm_for_sunrise_alert),
                        summary = stringResource(R.string.alarm_description),
                        checked = viewModel.useAlarmForSunrise,
                        onCheckedChange = viewModel::changeUseAlarmForSunrise,
                        position = position
                    )
                }
            }
        }

        trailSenseSection("events", title = eventsTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_meteor_showers_title),
                    checked = viewModel.meteorAlerts,
                    onCheckedChange = viewModel::changeMeteorAlerts,
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.lunar_eclipses),
                    checked = viewModel.lunarEclipseAlerts,
                    onCheckedChange = viewModel::changeLunarEclipseAlerts,
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.solar_eclipses),
                    checked = viewModel.solarEclipseAlerts,
                    onCheckedChange = viewModel::changeSolarEclipseAlerts,
                    position = position
                )
            }
        }
    }
}

private fun timeOptions(formatter: FormatService, minutes: List<Long>): List<SettingsOption> {
    return minutes.map {
        SettingsOption(it.toString(), formatter.formatDuration(Duration.ofMinutes(it)))
    }
}

/** The preference keys the astronomy settings write directly (resolved from resources by the screen). */
internal data class AstronomySettingsKeys(
    val sunTimeMode: String,
    val centerSunAndMoon: String,
    val quickActionLeft: String,
    val quickActionRight: String,
    val startCameraIn3D: String,
    val sunsetAlertTime: String,
    val sunriseAlertTime: String,
    val useAlarmForSunset: String,
    val useAlarmForSunrise: String
)

/** The astronomy settings rows, over preferences and the sunrise and sunset alert services. */
internal class AstronomySettingsViewModel(
    private val prefs: AstronomyPreferences,
    val cache: IPreferences,
    private val sunsetService: ToolService,
    private val sunriseService: ToolService,
    private val keys: AstronomySettingsKeys
) : ViewModel() {

    var sunTimeMode by mutableStateOf(cache.getString(keys.sunTimeMode) ?: "actual")
        private set
    var centerSunAndMoon by mutableStateOf(cache.getBoolean(keys.centerSunAndMoon) ?: false)
        private set
    var showAstronomyBands by mutableStateOf(prefs.showAstronomyBands)
        private set
    var leftQuickAction by mutableStateOf(prefs.leftButton.toString())
        private set
    var rightQuickAction by mutableStateOf(prefs.rightButton.toString())
        private set
    var startCameraIn3D by mutableStateOf(cache.getBoolean(keys.startCameraIn3D) ?: true)
        private set
    var sunsetAlerts by mutableStateOf(prefs.sendSunsetAlerts)
        private set
    var sunsetAlertTime by mutableStateOf(cache.getString(keys.sunsetAlertTime) ?: "60")
        private set
    var useAlarmForSunset by mutableStateOf(cache.getBoolean(keys.useAlarmForSunset) ?: false)
        private set
    var sunriseAlerts by mutableStateOf(prefs.sendSunriseAlerts)
        private set
    var sunriseAlertTime by mutableStateOf(cache.getString(keys.sunriseAlertTime) ?: "0")
        private set
    var useAlarmForSunrise by mutableStateOf(cache.getBoolean(keys.useAlarmForSunrise) ?: false)
        private set
    var meteorAlerts by mutableStateOf(prefs.sendMeteorShowerAlerts)
        private set
    var lunarEclipseAlerts by mutableStateOf(prefs.sendLunarEclipseAlerts)
        private set
    var solarEclipseAlerts by mutableStateOf(prefs.sendSolarEclipseAlerts)
        private set

    fun changeSunTimeMode(value: String) {
        sunTimeMode = value
        cache.putString(keys.sunTimeMode, value)
    }

    fun changeCenterSunAndMoon(value: Boolean) {
        centerSunAndMoon = value
        cache.putBoolean(keys.centerSunAndMoon, value)
    }

    fun changeShowAstronomyBands(value: Boolean) {
        showAstronomyBands = value
        prefs.showAstronomyBands = value
    }

    fun changeLeftQuickAction(value: String) {
        leftQuickAction = value
        cache.putString(keys.quickActionLeft, value)
    }

    fun changeRightQuickAction(value: String) {
        rightQuickAction = value
        cache.putString(keys.quickActionRight, value)
    }

    fun changeStartCameraIn3D(value: Boolean) {
        startCameraIn3D = value
        cache.putBoolean(keys.startCameraIn3D, value)
    }

    /** Saves the switch; turning alerts off disables the service here, turning them on is the screen's to do. */
    fun changeSunsetAlerts(enabled: Boolean) {
        sunsetAlerts = enabled
        prefs.sendSunsetAlerts = enabled
        if (!enabled) {
            viewModelScope.launch { sunsetService.disable() }
        }
    }

    fun changeSunsetAlertTime(value: String) {
        sunsetAlertTime = value
        cache.putString(keys.sunsetAlertTime, value)
        restart(sunsetService)
    }

    fun changeUseAlarmForSunset(value: Boolean) {
        useAlarmForSunset = value
        cache.putBoolean(keys.useAlarmForSunset, value)
    }

    /** Saves the switch; turning alerts off disables the service here, turning them on is the screen's to do. */
    fun changeSunriseAlerts(enabled: Boolean) {
        sunriseAlerts = enabled
        prefs.sendSunriseAlerts = enabled
        if (!enabled) {
            viewModelScope.launch { sunriseService.disable() }
        }
    }

    fun changeSunriseAlertTime(value: String) {
        sunriseAlertTime = value
        cache.putString(keys.sunriseAlertTime, value)
        restart(sunriseService)
    }

    fun changeUseAlarmForSunrise(value: Boolean) {
        useAlarmForSunrise = value
        cache.putBoolean(keys.useAlarmForSunrise, value)
    }

    fun changeMeteorAlerts(value: Boolean) {
        meteorAlerts = value
        prefs.sendMeteorShowerAlerts = value
    }

    fun changeLunarEclipseAlerts(value: Boolean) {
        lunarEclipseAlerts = value
        prefs.sendLunarEclipseAlerts = value
    }

    fun changeSolarEclipseAlerts(value: Boolean) {
        solarEclipseAlerts = value
        prefs.sendSolarEclipseAlerts = value
    }

    /** The alert switches follow the preferences when the alerts are turned on or off elsewhere. */
    fun reloadAlerts() {
        sunsetAlerts = prefs.sendSunsetAlerts
        sunriseAlerts = prefs.sendSunriseAlerts
    }

    fun onPreferenceChanged(key: String) {
        if (key == keys.sunsetAlertTime) {
            restart(sunsetService)
        } else if (key == keys.sunriseAlertTime) {
            restart(sunriseService)
        }
    }

    private fun restart(service: ToolService) {
        viewModelScope.launch { service.restart() }
    }
}
