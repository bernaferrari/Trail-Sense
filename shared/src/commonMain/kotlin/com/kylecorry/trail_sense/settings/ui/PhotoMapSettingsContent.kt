package com.kylecorry.trail_sense.settings.ui

import com.kylecorry.trail_sense.shared.text.AppStrings
import com.kylecorry.trail_sense.shared.dialogs.disclaimer
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.generated.resources.*
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.main.getAppService



import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection

/** Photo map settings. [onLayers] opens the platform's map layer settings; that row is hidden when null. */
@Composable
fun PhotoMapSettingsScreen(onLayers: (() -> Unit)?) {
    val host = AppHost.current()
    val prefs = remember { getAppService<UserPreferences>().photoMaps }
    var reducePhotoResolution by remember {
        mutableStateOf(prefs.autoReducePhotoMaps)
    }
    var reducePdfResolution by remember {
        mutableStateOf(prefs.autoReducePdfMaps)
    }
    var showMapPreviews by remember { mutableStateOf(prefs.showMapPreviews) }
    var keepMapFacingUp by remember { mutableStateOf(prefs.keepMapFacingUp) }
    var keepUnlocked by remember {
        mutableStateOf(prefs.keepScreenUnlockedWhileOpen)
    }
    var highDetailMode by remember { mutableStateOf(prefs.highDetailMode) }
    val importTitle = appString(Res.string.on_import)

    SettingsList(title = appString(Res.string.photo_maps)) {
        trailSenseSection("import", title = importTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.reduce_photo_resolution),
                    checked = reducePhotoResolution,
                    onCheckedChange = {
                        reducePhotoResolution = it
                        prefs.autoReducePhotoMaps = it
                        onReduceMapResolutionChange(host, reducePdfResolution)
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.reduce_pdf_resolution),
                    checked = reducePdfResolution,
                    onCheckedChange = {
                        reducePdfResolution = it
                        prefs.autoReducePdfMaps = it
                        onReduceMapResolutionChange(host, reducePhotoResolution)
                    },
                    position = position
                )
            }
        }
        trailSenseSection("display") {
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.pref_show_map_previews_title),
                    checked = showMapPreviews,
                    onCheckedChange = {
                        showMapPreviews = it
                        prefs.showMapPreviews = it
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.pref_keep_map_facing_up_title),
                    summary = appString(Res.string.pref_keep_map_facing_up_summary),
                    checked = keepMapFacingUp,
                    onCheckedChange = {
                        keepMapFacingUp = it
                        prefs.keepMapFacingUp = it
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.pref_navigation_lock_screen_when_open_title),
                    checked = keepUnlocked,
                    onCheckedChange = {
                        keepUnlocked = it
                        prefs.keepScreenUnlockedWhileOpen = it
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = appString(Res.string.pref_high_detail_mode_title),
                    summary = appString(Res.string.pref_high_detail_mode_summary),
                    checked = highDetailMode,
                    onCheckedChange = {
                        highDetailMode = it
                        prefs.highDetailMode = it
                    },
                    position = position
                )
            }
        }
        if (onLayers != null) trailSenseSection("layers") {
            row { position ->
                SettingsActionRow(
                    label = appString(Res.string.layers),
                    onClick = onLayers,
                    position = position
                )
            }
        }
    }
}


private fun onReduceMapResolutionChange(host: AppHost, shouldReduce: Boolean) {
    if (shouldReduce) {
        return
    }

    val strings = getAppService<AppStrings>()
    Alerts.disclaimer(
        host,
        strings.get(Res.string.reduce_map_resolution),
        strings.get(Res.string.reduce_map_resolution_crop_disclaimer),
        strings.get(Res.string.reduce_map_resolution_crop_disclaimer_shown),
        cancelText = null
    )
}
