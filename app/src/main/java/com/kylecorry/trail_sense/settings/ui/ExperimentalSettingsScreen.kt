package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.main.MainActivity

import android.content.Context

import androidx.activity.compose.LocalActivity

import androidx.compose.ui.platform.LocalContext

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.sense.Sensors
import com.kylecorry.sol.science.meteorology.forecast.ForecastSource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.alerts.NotificationSubsystem.GroupBehavior
import com.kylecorry.trail_sense.shared.debugging.isDebug
import com.kylecorry.trail_sense.shared.preferences.PreferencesSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools

@Composable
fun ExperimentalSettingsToolScreen() {
    val context = LocalContext.current
    // The bottom navigation belongs to the activity; it redraws its tools when this tool appears or goes.
    val mainActivity = LocalActivity.current as? MainActivity
    val cache = remember { PreferencesSubsystem.getInstance(context).preferences }
    val userPrefs = remember { UserPreferences(context) }
    val hasMetalDirectionSupport = remember {
        Sensors.hasGyroscope(context) && SensorService(context).hasCompass()
    }
    val showForecastSource = remember {
        Tools.isToolAvailable(context, Tools.WEATHER) && isDebug()
    }
    val showNotificationGrouping = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
    }
    val forecastOptions = remember { forecastOptions() }
    val notificationGroupingOptions = remember { context.notificationGroupingOptions() }

    var showMetalDirection by remember {
        mutableStateOf(context.getBoolean(cache, R.string.pref_experimental_metal_direction, false))
    }
    var useZeroShutterLag by remember {
        mutableStateOf(userPrefs.camera.useZeroShutterLag)
    }
    var isCliffHeightEnabled by remember {
        mutableStateOf(userPrefs.isCliffHeightEnabled)
    }
    var useFilteredGps by remember {
        mutableStateOf(userPrefs.gps.smoothing > 0)
    }
    var forecastSource by remember {
        mutableStateOf(
            context.getStringPreference(
                cache,
                R.string.pref_weather_forecast_source,
                forecastSourceValue(ForecastSource.Sol)
            )
        )
    }
    var showPressureForecast by remember {
        mutableStateOf(userPrefs.weather.showPressureForecast)
    }
    var notificationGrouping by remember {
        mutableStateOf(notificationGroupingValue(userPrefs.notificationGroupingBehavior))
    }

    val toolsTitle = stringResource(R.string.tools)
    val cameraTitle = stringResource(R.string.camera)
    val gpsTitle = stringResource(R.string.gps)
    val weatherTitle = stringResource(R.string.weather)
    val notificationsTitle = stringResource(R.string.settings_notifications_section)

    // Each experiment sits under the part of the app it changes, so no row needs to say where it applies.
    SettingsList(title = stringResource(R.string.experimental)) {
        trailSenseSection("tools", title = toolsTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.tool_cliff_height_title),
                    summary = stringResource(R.string.tool_cliff_height_description),
                    checked = isCliffHeightEnabled,
                    onCheckedChange = {
                        isCliffHeightEnabled = it
                        userPrefs.isCliffHeightEnabled = it
                        mainActivity?.updateBottomNavigation()
                    },
                    position = position
                )
            }
            if (hasMetalDirectionSupport) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_experimental_metal_direction_title),
                        summary = stringResource(R.string.tool_metal_detector_title),
                        checked = showMetalDirection,
                        onCheckedChange = {
                            showMetalDirection = it
                            context.putBoolean(cache, R.string.pref_experimental_metal_direction, it)
                        },
                        position = position
                    )
                }
            }
        }
        trailSenseSection("camera", title = cameraTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.zero_shutter_lag),
                    checked = useZeroShutterLag,
                    onCheckedChange = {
                        useZeroShutterLag = it
                        context.putBoolean(cache, R.string.pref_use_zero_shutter_lag, it)
                    },
                    position = position
                )
            }
        }
        trailSenseSection("gps", title = gpsTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.smooth_gps_readings),
                    checked = useFilteredGps,
                    onCheckedChange = {
                        useFilteredGps = it
                        cache.putInt(context.getString(R.string.pref_gps_smoothing), if (it) 50 else 0)
                    },
                    position = position
                )
            }
        }
        trailSenseSection("weather", title = weatherTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.show_pressure_forecast),
                    checked = showPressureForecast,
                    onCheckedChange = {
                        showPressureForecast = it
                        context.putBoolean(cache, R.string.pref_show_pressure_forecast, it)
                    },
                    position = position
                )
            }
            if (showForecastSource) {
                row { position ->
                    UnitChoiceRow(
                        label = stringResource(R.string.forecast_algorithm),
                        options = forecastOptions,
                        selected = forecastSource,
                        onSelected = {
                            forecastSource = it
                            context.putString(cache, R.string.pref_weather_forecast_source, it)
                        },
                        position = position
                    )
                }
            }
        }
        if (showNotificationGrouping) {
            trailSenseSection("notifications", title = notificationsTitle) {
                row { position ->
                    UnitPickerRow(
    label = stringResource(R.string.notification_grouping),
                        options = notificationGroupingOptions,
                        selected = notificationGrouping,
                        onSelected = {
                            notificationGrouping = it
                            context.putString(cache, R.string.pref_notification_group_behavior, it)
                        },
                        position = position
                    )
                }
            }
        }
    }
}

private fun forecastOptions(): List<SettingsOption> {
    return ForecastSource.entries.map {
        SettingsOption(forecastSourceValue(it), it.name)
    }
}

private fun Context.notificationGroupingOptions(): List<SettingsOption> {
    val labels = mapOf(
        GroupBehavior.System to getString(R.string.theme_system),
        GroupBehavior.UngroupAll to getString(R.string.ungroup_all),
        GroupBehavior.UngroupHigh to getString(R.string.ungroup_alerts)
    )
    return GroupBehavior.entries.map {
        SettingsOption(
            notificationGroupingValue(it),
            labels[it] ?: it.name
        )
    }
}

private fun forecastSourceValue(source: ForecastSource): String {
    return when (source) {
        ForecastSource.Sol -> "1"
        ForecastSource.Zambretti -> "2"
    }
}

private fun notificationGroupingValue(behavior: GroupBehavior): String {
    return when (behavior) {
        GroupBehavior.System -> "1"
        GroupBehavior.UngroupAll -> "2"
        GroupBehavior.UngroupHigh -> "3"
    }
}
