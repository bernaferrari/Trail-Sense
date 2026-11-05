package com.kylecorry.trail_sense.settings.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.receivers.StartOnBootController
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.tiles.TileManager
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryLogWorker
import com.kylecorry.trail_sense.tools.battery.infrastructure.LowPowerMode

@Composable
fun PowerSettingsToolScreen(host: TrailSenseDestinationHost) {
    val prefs = remember { UserPreferences(host.requireContext()) }
    var lowPowerMode by remember { mutableStateOf(prefs.isLowPowerModeOn) }
    var autoLowPower by remember { mutableStateOf(prefs.power.autoLowPower) }
    var startOnBoot by remember { mutableStateOf(prefs.power.startOnBoot) }
    var tilesEnabled by remember { mutableStateOf(prefs.power.areTilesEnabled) }
    var batteryLogEnabled by remember { mutableStateOf(prefs.power.enableBatteryLog) }
    val backgroundTitle = stringResource(R.string.background)

    SettingsList(host, title = stringResource(R.string.tool_battery_title)) {
        trailSenseSection("power_saving") {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_low_power_mode_title),
                    summary = stringResource(R.string.pref_low_power_mode_summary),
                    checked = lowPowerMode,
                    onCheckedChange = {
                        lowPowerMode = it
                        host.setLowPowerMode(prefs, it)
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_auto_low_power_title),
                    summary = stringResource(R.string.pref_auto_low_power_summary),
                    checked = autoLowPower,
                    onCheckedChange = {
                        autoLowPower = it
                        prefs.power.autoLowPower = it
                    },
                    position = position
                )
            }
        }
        trailSenseSection("background", title = backgroundTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_start_on_boot_title),
                    checked = startOnBoot,
                    onCheckedChange = {
                        startOnBoot = it
                        prefs.power.startOnBoot = it
                        host.setStartOnBoot(it)
                    },
                    position = position
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                row { position ->
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_tiles_enabled_title),
                        summary = stringResource(R.string.pref_tiles_enabled_summary),
                        checked = tilesEnabled,
                        onCheckedChange = {
                            tilesEnabled = it
                            prefs.power.areTilesEnabled = it
                            TileManager().setTilesEnabled(host.requireContext(), it)
                        },
                        position = position
                    )
                }
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_tiles_battery_log),
                    summary = stringResource(R.string.pref_tiles_enabled_battery_log_summary),
                    checked = batteryLogEnabled,
                    onCheckedChange = {
                        batteryLogEnabled = it
                        prefs.power.enableBatteryLog = it
                        BatteryLogWorker.enableBatteryLog(host.requireContext(), it)
                    },
                    position = position
                )
            }
        }
    }
}

private fun TrailSenseDestinationHost.setLowPowerMode(prefs: UserPreferences, enabled: Boolean) {
    if (enabled) {
        prefs.power.userEnabledLowPower = true
        LowPowerMode(requireContext()).enable()
    } else {
        prefs.power.userEnabledLowPower = false
        LowPowerMode(requireContext()).disable()
    }
}

private fun TrailSenseDestinationHost.setStartOnBoot(enabled: Boolean) {
    val controller = StartOnBootController(requireContext())
    if (enabled) {
        controller.enable()
    } else {
        controller.disable()
    }
}
