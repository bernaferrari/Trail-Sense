package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.main.getAppService

import com.kylecorry.trail_sense.tools.flashlight.infrastructure.IFlashlightSubsystem

import kotlin.time.Duration


import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kylecorry.trail_sense.shared.FormatService
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.DurationPickerDialog
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

@Composable
fun FlashlightSettingsToolScreen() {
    val formatter = remember { FormatService.getInstance() }
    val prefs = remember { getAppService<UserPreferences>().flashlight }
    val hasFlashlight = remember {
        getAppService<IFlashlightSubsystem>().isAvailable()
    }
    var toggleWithVolume by remember {
        mutableStateOf(prefs.toggleWithVolumeButtons)
    }
    var shouldTimeout by remember { mutableStateOf(prefs.shouldTimeout) }
    var timeout by remember { mutableStateOf(prefs.timeout) }
    var controlScreenWithVolume by remember {
        mutableStateOf(prefs.controlScreenFlashlightWithVolumeButtons)
    }
    var showTimeoutPicker by remember { mutableStateOf(false) }
    val timeoutTitle = appString(Res.string.pref_flashlight_timeout_title)
    val screenTitle = appString(Res.string.screen_flashlight)

    SettingsList(title = appString(Res.string.flashlight_title)) {
        // Without a torch only the screen flashlight is left, so its header would have nothing to set it apart.
        if (hasFlashlight) {
            trailSenseSection("torch") {
                row { position ->
                    SettingsSwitchRow(
                        label = appString(Res.string.pref_flashlight_toggle_with_volume_title),
                        summary = appString(Res.string.pref_flashlight_toggle_with_volume_summary),
                        checked = toggleWithVolume,
                        onCheckedChange = {
                            toggleWithVolume = it
                            prefs.toggleWithVolumeButtons = it
                        },
                        position = position
                    )
                }
                row { position ->
                    SettingsSwitchRow(
                        label = appString(Res.string.pref_flashlight_should_timeout_title),
                        checked = shouldTimeout,
                        onCheckedChange = {
                            shouldTimeout = it
                            prefs.shouldTimeout = it
                        },
                        position = position
                    )
                }
                if (shouldTimeout) {
                    row { position ->
                        SettingsActionRow(
                            label = timeoutTitle,
                            summary = formatter.formatDuration(
                                timeout,
                                short = false,
                                includeSeconds = true
                            ),
                            onClick = {
                                showTimeoutPicker = true
                            },
                            position = position
                        )
                    }
                }
            }
        }
        trailSenseSection("screen", title = screenTitle.takeIf { hasFlashlight }) {
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.pref_flashlight_control_screen_with_volume_title),
                    checked = controlScreenWithVolume,
                    onCheckedChange = {
                        controlScreenWithVolume = it
                        prefs.controlScreenFlashlightWithVolumeButtons = it
                    },
                    position = position
                )
            }
        }
    }

    if (showTimeoutPicker) {
        DurationPickerDialog(
            title = timeoutTitle,
            duration = timeout,
            showSeconds = true,
            onDurationPicked = {
                if (it != null && it != Duration.ZERO) {
                    prefs.timeout = it
                    timeout = it
                }
                showTimeoutPicker = false
            },
            onDismiss = {
                showTimeoutPicker = false
            }
        )
    }
}
