package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

import android.content.Context
import com.kylecorry.trail_sense.R

object DefaultMapLayerDefinitions {
    fun getBasePreferences(
        context: Context,
        layerName: String
    ): List<MapLayerPreference> {
        return listOf(
            MapLayerPreference(
                ENABLED,
                layerName,
                MapLayerPreferenceType.Switch,
                defaultValue = true,
                dependency = null
            ),
            MapLayerPreference(
                OPACITY,
                context.getString(R.string.opacity),
                MapLayerPreferenceType.Seekbar,
                defaultValue = 100
            )
        )
    }

    const val ENABLED = MapLayerPreferenceKeys.ENABLED
    const val OPACITY = MapLayerPreferenceKeys.OPACITY
    const val DEFAULT_OPACITY = MapLayerPreferenceKeys.DEFAULT_OPACITY
    const val BACKGROUND_COLOR = MapLayerPreferenceKeys.BACKGROUND_COLOR
    const val SHOW_LABELS = MapLayerPreferenceKeys.SHOW_LABELS
}
