package com.kylecorry.trail_sense.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString

@Composable
fun CellSignalSettingsToolScreen() {
    val prefs = remember { getAppService<UserPreferences>().cellSignal }
    var populateCache by remember { mutableStateOf(prefs.populateCache) }
    SingleSwitchSettingsScreen(
        title = appString(Res.string.cell_signal),
        label = appString(Res.string.pref_cell_signal_refresh_cache_title),
        summary = appString(Res.string.pref_cell_signal_refresh_cache_summary),
        checked = populateCache,
        onCheckedChange = {
            populateCache = it
            prefs.populateCache = it
        },
        onBack = rememberGoBack()
    )
}

@Composable
fun ClockSettingsToolScreen() {
    val prefs = remember { getAppService<UserPreferences>().clock }
    var showAnalogClock by remember { mutableStateOf(prefs.showAnalogClock) }
    SingleSwitchSettingsScreen(
        title = appString(Res.string.tool_clock_title),
        label = appString(Res.string.pref_enable_analog_title),
        checked = showAnalogClock,
        onCheckedChange = {
            showAnalogClock = it
            prefs.showAnalogClock = it
        },
        onBack = rememberGoBack()
    )
}

@Composable
fun TideSettingsToolScreen() {
    val prefs = remember { getAppService<UserPreferences>().tides }
    var showNearestTide by remember { mutableStateOf(prefs.showNearestTide) }
    SingleSwitchSettingsScreen(
        title = appString(Res.string.tides),
        label = appString(Res.string.pref_show_nearest_tide_title),
        checked = showNearestTide,
        onCheckedChange = {
            showNearestTide = it
            prefs.showNearestTide = it
        },
        onBack = rememberGoBack()
    )
}

@Composable
fun TurnBackSettingsToolScreen() {
    val prefs = remember { getAppService<UserPreferences>().turnBack }
    var useAlarm by remember { mutableStateOf(prefs.useAlarm) }
    SingleSwitchSettingsScreen(
        title = appString(Res.string.tool_turn_back),
        label = appString(Res.string.use_alarm_for_turn_back_alert),
        footer = appString(Res.string.alarm_description),
        checked = useAlarm,
        onCheckedChange = {
            useAlarm = it
            prefs.useAlarm = it
        },
        onBack = rememberGoBack()
    )
}

@Composable
fun WaterBoilSettingsToolScreen() {
    val prefs = remember { getAppService<UserPreferences>().waterBoilTimer }
    var useAlarm by remember { mutableStateOf(prefs.useAlarm) }
    SingleSwitchSettingsScreen(
        title = appString(Res.string.water_boil_timer),
        label = appString(Res.string.use_alarm_for_water_boil_timer),
        footer = appString(Res.string.alarm_description),
        checked = useAlarm,
        onCheckedChange = {
            useAlarm = it
            prefs.useAlarm = it
        },
        onBack = rememberGoBack()
    )
}
