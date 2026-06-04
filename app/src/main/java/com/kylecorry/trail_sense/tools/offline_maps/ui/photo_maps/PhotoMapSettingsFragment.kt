package com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps

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
import com.kylecorry.trail_sense.shared.CustomUiUtils
import com.kylecorry.trail_sense.shared.UserPreferences
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersBottomSheet
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseComposeFragment
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDivider
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSectionHeader
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import com.kylecorry.trail_sense.tools.offline_maps.OfflineMapsToolRegistration

class PhotoMapSettingsFragment : TrailSenseComposeFragment() {

    private var layerSheet: MapLayersBottomSheet? = null
    private val prefs by lazy { UserPreferences(requireContext()).photoMaps }

    @Composable
    override fun FragmentContent() {
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
                    TrailSenseSectionHeader(title = stringResource(R.string.photo_maps))
                }
                item("reduce_photo") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.reduce_photo_resolution),
                        summary = stringResource(R.string.on_import),
                        checked = reducePhotoResolution,
                        onCheckedChange = {
                            reducePhotoResolution = it
                            prefs.autoReducePhotoMaps = it
                            onReduceResolutionChange(reducePdfResolution)
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                item("reduce_pdf") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.reduce_pdf_resolution),
                        summary = stringResource(R.string.on_import),
                        checked = reducePdfResolution,
                        onCheckedChange = {
                            reducePdfResolution = it
                            prefs.autoReducePdfMaps = it
                            onReduceResolutionChange(reducePhotoResolution)
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                item("previews") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_show_map_previews_title),
                        checked = showMapPreviews,
                        onCheckedChange = {
                            showMapPreviews = it
                            prefs.showMapPreviews = it
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
                }
                item("facing_up") {
                    SettingsSwitchRow(
                        label = stringResource(R.string.pref_keep_map_facing_up_title),
                        summary = stringResource(R.string.pref_keep_map_facing_up_summary),
                        checked = keepMapFacingUp,
                        onCheckedChange = {
                            keepMapFacingUp = it
                            prefs.keepMapFacingUp = it
                        },
                        modifier = Modifier.padding(horizontal = TrailSenseSpacing.lg)
                    )
                    TrailSenseDivider()
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

    private fun onReduceResolutionChange(shouldReduce: Boolean) {
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

    override fun onPause() {
        super.onPause()
        layerSheet?.dismiss()
    }

    private fun showLayerSheet() {
        layerSheet?.dismiss()
        layerSheet = MapLayersBottomSheet(
            OfflineMapsToolRegistration.PHOTO_MAPS_ID,
            PhotoMapToolLayerManager.alwaysEnabledLayers
        )
        layerSheet?.show(this)
    }
}
