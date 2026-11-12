package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import android.content.Context
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.trail_sense.shared.map_layers.MapLayerLoader
import com.kylecorry.trail_sense.shared.map_layers.getAttribution
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.DefaultMapLayerDefinitions
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo

suspend fun IMapView.setLayersWithPreferences(
    mapId: String,
    layerIds: List<String>,
    additionalLayers: List<ILayer> = emptyList(),
    forceReplaceLayers: Boolean = false
) {
    val loader = DependencyRegistry.get<MapLayerLoader>()
    val repo = DependencyRegistry.get<MapLayerPreferenceRepo>()
    val currentLayers = layerManager.getLayers()
    val newLayerIds = layerIds + additionalLayers.map { it.layerId }
    val definitions = loader.getDefinitions()
    val layers = if (!forceReplaceLayers && currentLayers.map { it.layerId } == newLayerIds) {
        currentLayers
    } else {
        layerIds.mapNotNull { id ->
            loader.getLayer(id, definitions)
        } + additionalLayers
    }

    val layerPreferences = repo.getLayerPreferenceValues(mapId, newLayerIds).mapValues { (layerId, values) ->
        val defaults = definitions[layerId]?.preferences
            ?.associate { it.id to it.defaultValue.takeIf(::isLayerPreferenceValue) }
            ?: emptyMap()
        values.withDefaults(defaults)
    }

    val layersToPreference = layers.map { layer ->
        layer to layerPreferences[layer.layerId]
    }
    val actualLayers =
        layersToPreference.filter {
            it.second?.containsKey(DefaultMapLayerDefinitions.ENABLED) == false ||
                    it.second?.getBoolean(DefaultMapLayerDefinitions.ENABLED) != false
        }
    actualLayers.forEach {
        it.second?.let { prefs ->
            it.first.setPreferences(prefs)
            it.first.invalidate()
        }
    }

    setLayers(actualLayers.map { it.first })
}

/** The value types a layer preference can hold (any other default is left unset). */
private fun isLayerPreferenceValue(value: Any?): Boolean {
    return value is Boolean || value is Int || value is Float || value is Long || value is String
}

suspend fun IMapView.getAttribution(context: Context): CharSequence? {
    val loader = DependencyRegistry.get<MapLayerLoader>()
    return loader.getAttribution(context, layerManager.getLayers().map { it.layerId })
}
