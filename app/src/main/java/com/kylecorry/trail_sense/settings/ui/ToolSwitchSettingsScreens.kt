package com.kylecorry.trail_sense.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost

@Composable
fun CellSignalSettingsToolScreen(host: TrailSenseDestinationHost) {
    val prefs = remember { UserPreferences(host.requireContext()).cellSignal }
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
        onBack = host::goBack
    )
}

@Composable
fun ClockSettingsToolScreen(host: TrailSenseDestinationHost) {
    val prefs = remember { UserPreferences(host.requireContext()).clock }
    var showAnalogClock by remember { mutableStateOf(prefs.showAnalogClock) }
    SingleSwitchSettingsScreen(
        title = stringResource(R.string.tool_clock_title),
        label = stringResource(R.string.pref_enable_analog_title),
        checked = showAnalogClock,
        onCheckedChange = {
            showAnalogClock = it
            prefs.showAnalogClock = it
        },
        onBack = host::goBack
    )
}

@Composable
fun TideSettingsToolScreen(host: TrailSenseDestinationHost) {
    val prefs = remember { UserPreferences(host.requireContext()).tides }
    var showNearestTide by remember { mutableStateOf(prefs.showNearestTide) }
    SingleSwitchSettingsScreen(
        title = stringResource(R.string.tides),
        label = stringResource(R.string.pref_show_nearest_tide_title),
        checked = showNearestTide,
        onCheckedChange = {
            showNearestTide = it
            prefs.showNearestTide = it
        },
        onBack = host::goBack
    )
}

@Composable
fun TurnBackSettingsToolScreen(host: TrailSenseDestinationHost) {
    val prefs = remember { UserPreferences(host.requireContext()).turnBack }
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
        onBack = host::goBack
    )
}

@Composable
fun WaterBoilSettingsToolScreen(host: TrailSenseDestinationHost) {
    val prefs = remember { UserPreferences(host.requireContext()).waterBoilTimer }
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
        onBack = host::goBack
    )
}
