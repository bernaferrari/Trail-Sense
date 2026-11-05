package com.kylecorry.trail_sense.settings.ui

import androidx.compose.ui.platform.LocalContext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences

@Composable
fun CellSignalSettingsToolScreen() {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context).cellSignal }
    var populateCache by remember { mutableStateOf(prefs.populateCache) }
    SingleSwitchSettingsScreen(
        title = stringResource(R.string.cell_signal),
        label = stringResource(R.string.pref_cell_signal_refresh_cache_title),
        summary = stringResource(R.string.pref_cell_signal_refresh_cache_summary),
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
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context).clock }
    var showAnalogClock by remember { mutableStateOf(prefs.showAnalogClock) }
    SingleSwitchSettingsScreen(
        title = stringResource(R.string.tool_clock_title),
        label = stringResource(R.string.pref_enable_analog_title),
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
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context).tides }
    var showNearestTide by remember { mutableStateOf(prefs.showNearestTide) }
    SingleSwitchSettingsScreen(
        title = stringResource(R.string.tides),
        label = stringResource(R.string.pref_show_nearest_tide_title),
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
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context).turnBack }
    var useAlarm by remember { mutableStateOf(prefs.useAlarm) }
    SingleSwitchSettingsScreen(
        title = stringResource(R.string.tool_turn_back),
        label = stringResource(R.string.use_alarm_for_turn_back_alert),
        footer = stringResource(R.string.alarm_description),
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
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context).waterBoilTimer }
    var useAlarm by remember { mutableStateOf(prefs.useAlarm) }
    SingleSwitchSettingsScreen(
        title = stringResource(R.string.water_boil_timer),
        label = stringResource(R.string.use_alarm_for_water_boil_timer),
        footer = stringResource(R.string.alarm_description),
        checked = useAlarm,
        onCheckedChange = {
            useAlarm = it
            prefs.useAlarm = it
        },
        onBack = rememberGoBack()
    )
}
