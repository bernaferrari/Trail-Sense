package com.kylecorry.trail_sense.settings.ui

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.receivers.StartOnBootController
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.shared.tiles.TileManager
import com.kylecorry.trail_sense.tools.battery.infrastructure.BatteryLogWorker
import com.kylecorry.trail_sense.tools.battery.infrastructure.LowPowerMode

class PowerSettingsFragment : Fragment() {

    private val prefs by lazy { UserPreferences(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TrailSenseTheme {
                    var lowPowerMode by remember { mutableStateOf(prefs.isLowPowerModeOn) }
                    var autoLowPower by remember { mutableStateOf(prefs.power.autoLowPower) }
                    var startOnBoot by remember { mutableStateOf(prefs.power.startOnBoot) }
                    var tilesEnabled by remember { mutableStateOf(prefs.power.areTilesEnabled) }
                    var batteryLogEnabled by remember {
                        mutableStateOf(prefs.power.enableBatteryLog)
                    }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = TrailSenseSpacing.lg,
                                end = TrailSenseSpacing.lg,
                                top = TrailSenseSpacing.lg,
                                bottom = TrailSenseSpacing.xxxl
                            )
                        ) {
                            item("header") {
                                TrailSenseSectionHeader(
                                    title = stringResource(R.string.tool_battery_title)
                                )
                            }
                            item("low_power") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_low_power_mode_title),
                                    summary = stringResource(R.string.pref_low_power_mode_summary),
                                    checked = lowPowerMode,
                                    onCheckedChange = {
                                        lowPowerMode = it
                                        setLowPowerMode(it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("auto_low_power") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_auto_low_power_title),
                                    summary = stringResource(R.string.pref_auto_low_power_summary),
                                    checked = autoLowPower,
                                    onCheckedChange = {
                                        autoLowPower = it
                                        prefs.power.autoLowPower = it
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            item("start_on_boot") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_start_on_boot_title),
                                    checked = startOnBoot,
                                    onCheckedChange = {
                                        startOnBoot = it
                                        prefs.power.startOnBoot = it
                                        setStartOnBoot(it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                item("tiles") {
                                    SettingsSwitchRow(
                                        label = stringResource(R.string.pref_tiles_enabled_title),
                                        summary = stringResource(R.string.pref_tiles_enabled_summary),
                                        checked = tilesEnabled,
                                        onCheckedChange = {
                                            tilesEnabled = it
                                            prefs.power.areTilesEnabled = it
                                            TileManager().setTilesEnabled(requireContext(), it)
                                        },
                                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                    )
                                    TrailSenseDivider()
                                }
                            }
                            item("battery_log") {
                                SettingsSwitchRow(
                                    label = stringResource(R.string.pref_tiles_battery_log),
                                    summary = stringResource(R.string.pref_tiles_enabled_battery_log_summary),
                                    checked = batteryLogEnabled,
                                    onCheckedChange = {
                                        batteryLogEnabled = it
                                        prefs.power.enableBatteryLog = it
                                        BatteryLogWorker.enableBatteryLog(requireContext(), it)
                                    },
                                    modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                                )
                                TrailSenseDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setLowPowerMode(enabled: Boolean) {
        if (enabled) {
            prefs.power.userEnabledLowPower = true
            LowPowerMode(requireContext()).enable()
        } else {
            prefs.power.userEnabledLowPower = false
            LowPowerMode(requireContext()).disable()
        }
    }

    private fun setStartOnBoot(enabled: Boolean) {
        val controller = StartOnBootController(requireContext())
        if (enabled) {
            controller.enable()
        } else {
            controller.disable()
        }
    }
}
