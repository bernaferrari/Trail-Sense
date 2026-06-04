package com.kylecorry.trail_sense.tools.map.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kylecorry.andromeda.fragments.show
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.settings.ui.SettingsActionRow
import com.kylecorry.trail_sense.settings.ui.SettingsSwitchRow
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersBottomSheet
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.map.MapToolRegistration

class MapSettingsFragment : TrailSenseComposeFragment() {

    private var layerSheet: MapLayersBottomSheet? = null
    private val prefs by lazy { UserPreferences(requireContext()).map }

    @Composable
    override fun FragmentContent() {
        var keepUnlocked by remember {
            mutableStateOf(prefs.keepScreenUnlockedWhileOpen)
        }
        var saveMapState by remember { mutableStateOf(prefs.saveMapState) }
        var highDetailMode by remember { mutableStateOf(prefs.highDetailMode) }
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
                    TrailSenseSectionHeader(title = stringResource(R.string.maps))
                }
                item("keep_unlocked") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_navigation_lock_screen_when_open_title),
                        checked = keepUnlocked,
                        onCheckedChange = {
                            keepUnlocked = it
                            prefs.keepScreenUnlockedWhileOpen = it
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                item("save_state") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.remember_map_state),
                        checked = saveMapState,
                        onCheckedChange = {
                            saveMapState = it
                            prefs.saveMapState = it
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                item("high_detail") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_high_detail_mode_title),
                        summary = stringResource(R.string.pref_high_detail_mode_summary),
                        checked = highDetailMode,
                        onCheckedChange = {
                            highDetailMode = it
                            prefs.highDetailMode = it
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                item("layers") {
                    SettingsActionRow(
                        label = stringResource(R.string.layers),
                        onClick = ::showLayerSheet,
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        layerSheet?.dismiss()
    }

    private fun showLayerSheet() {
        layerSheet?.dismiss()
        layerSheet = MapLayersBottomSheet(MapToolRegistration.MAP_ID)
        layerSheet?.show(this)
    }
}
