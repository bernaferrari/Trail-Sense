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
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
import com.kylecorry.trail_sense.shared.views.compose.trailSenseSection
import com.kylecorry.trail_sense.tools.map.MapToolRegistration

@Composable
fun MapSettingsToolScreen() {
    val context = LocalContext.current
    val layerSheet = rememberSettingsLayerSheet {
        MapLayersSheetState(MapToolRegistration.MAP_ID)
    }
    val prefs = remember { UserPreferences(context).map }
    var keepUnlocked by remember {
        mutableStateOf(prefs.keepScreenUnlockedWhileOpen)
    }
    var saveMapState by remember { mutableStateOf(prefs.saveMapState) }
    var highDetailMode by remember { mutableStateOf(prefs.highDetailMode) }

    SettingsList(title = stringResource(R.string.maps)) {
        trailSenseSection("display") {
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
                    label = stringResource(R.string.remember_map_state),
                    checked = saveMapState,
                    onCheckedChange = {
                        saveMapState = it
                        prefs.saveMapState = it
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
