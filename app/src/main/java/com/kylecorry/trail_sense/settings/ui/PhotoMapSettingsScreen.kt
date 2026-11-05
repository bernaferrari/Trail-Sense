package com.kylecorry.trail_sense.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDestinationHost
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.offline_maps.OfflineMapsToolRegistration
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.PhotoMapToolLayerManager

@Composable
fun PhotoMapSettingsToolScreen(host: TrailSenseDestinationHost) {
    val layerSheet = rememberSettingsLayerSheet(host) {
        MapLayersSheetState(
            OfflineMapsToolRegistration.PHOTO_MAPS_ID,
            PhotoMapToolLayerManager.alwaysEnabledLayers
        )
    }
    val prefs = remember { UserPreferences(host.requireContext()).photoMaps }
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
    val importTitle = stringResource(R.string.on_import)

    SettingsList(host, title = stringResource(R.string.photo_maps)) {
        trailSenseSection("import", title = importTitle) {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.reduce_photo_resolution),
                    checked = reducePhotoResolution,
                    onCheckedChange = {
                        reducePhotoResolution = it
                        prefs.autoReducePhotoMaps = it
                        host.onReduceMapResolutionChange(reducePdfResolution)
                    },
                    position = position
                )
            }
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.reduce_pdf_resolution),
                    checked = reducePdfResolution,
                    onCheckedChange = {
                        reducePdfResolution = it
                        prefs.autoReducePdfMaps = it
                        host.onReduceMapResolutionChange(reducePhotoResolution)
                    },
                    position = position
                )
            }
        }
        trailSenseSection("display") {
            row { position ->
                SettingsSwitchRow(
                    label = stringResource(R.string.pref_show_map_previews_title),
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
                    label = stringResource(R.string.pref_keep_map_facing_up_title),
                    summary = stringResource(R.string.pref_keep_map_facing_up_summary),
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
                    label = stringResource(R.string.pref_navigation_lock_screen_when_open_title),
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
                    label = stringResource(R.string.pref_high_detail_mode_title),
                    summary = stringResource(R.string.pref_high_detail_mode_summary),
                    checked = highDetailMode,
                    onCheckedChange = {
                        highDetailMode = it
                        prefs.highDetailMode = it
                    },
                    position = position
                )
            }
        }
        trailSenseSection("layers") {
            row { position ->
                SettingsActionRow(
                    label = stringResource(R.string.layers),
                    onClick = layerSheet::show,
                    position = position
                )
            }
        }
    }
    layerSheet.Content()
}


private fun TrailSenseDestinationHost.onReduceMapResolutionChange(shouldReduce: Boolean) {
    if (shouldReduce) {
        return
    }

    CustomUiUtils.disclaimer(
        requireContext(),
        getString(R.string.reduce_map_resolution),
        getString(R.string.reduce_map_resolution_crop_disclaimer),
        getString(R.string.reduce_map_resolution_crop_disclaimer_shown),
        cancelText = null
    )
}
