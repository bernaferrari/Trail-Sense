package com.kylecorry.trail_sense.settings.ui

import androidx.compose.runtime.Composable
import com.kylecorry.trail_sense.shared.map_layers.preferences.ui.MapLayersSheetState
import com.kylecorry.trail_sense.tools.offline_maps.OfflineMapsToolRegistration
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.PhotoMapToolLayerManager

/** Photo map settings on Android, with the View-based map layer sheet. */
@Composable
fun PhotoMapSettingsToolScreen() {
    val layerSheet = rememberSettingsLayerSheet {
        MapLayersSheetState(
            OfflineMapsToolRegistration.PHOTO_MAPS_ID,
            PhotoMapToolLayerManager.alwaysEnabledLayers
        )
    }
    PhotoMapSettingsScreen(onLayers = layerSheet::show)
    layerSheet.Content()
}
