package com.kylecorry.trail_sense.settings.ui

import android.content.Context

import androidx.compose.ui.platform.LocalContext

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
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryLogWorker
import com.kylecorry.trail_sense.tools.battery.infrastructure.LowPowerMode

@Composable
fun PowerSettingsToolScreen() {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    var lowPowerMode by remember { mutableStateOf(prefs.isLowPowerModeOn) }
    var autoLowPower by remember { mutableStateOf(prefs.power.autoLowPower) }
    var startOnBoot by remember { mutableStateOf(prefs.power.startOnBoot) }
    var tilesEnabled by remember { mutableStateOf(prefs.power.areTilesEnabled) }
    var batteryLogEnabled by remember { mutableStateOf(prefs.power.enableBatteryLog) }
    val backgroundTitle = stringResource(R.string.background)

    SettingsList(title = stringResource(R.string.tool_battery_title)) {
        trailSenseSection("power_saving") {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_low_power_mode_title),
                    summary = stringResource(R.string.pref_low_power_mode_summary),
                    checked = lowPowerMode,
                    onCheckedChange = {
                        lowPowerMode = it
                        context.setLowPowerMode(prefs, it)
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
                        context.setStartOnBoot(it)
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
                            TileManager().setTilesEnabled(context, it)
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
                        BatteryLogWorker.enableBatteryLog(context, it)
                    },
                    position = position
                )
            }
        }
    }
}

private fun Context.setLowPowerMode(prefs: UserPreferences, enabled: Boolean) {
    if (enabled) {
        prefs.power.userEnabledLowPower = true
        LowPowerMode(this).enable()
    } else {
        prefs.power.userEnabledLowPower = false
        LowPowerMode(this).disable()
    }
}

private fun Context.setStartOnBoot(enabled: Boolean) {
    val controller = StartOnBootController(this)
    if (enabled) {
        controller.enable()
    } else {
        controller.disable()
    }
}
