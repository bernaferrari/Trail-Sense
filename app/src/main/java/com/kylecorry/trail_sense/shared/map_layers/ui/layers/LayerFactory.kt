package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.AndroidMapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerType
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileMapLayer

class LayerFactory {

    fun createLayer(definition: AndroidMapLayerDefinition): ILayer {
        definition.layer?.let { return it() }

        return when (definition.layerType) {
            MapLayerType.Overlay -> throw IllegalStateException("Overlays must provide a layer definition")
            MapLayerType.Feature -> GeoJsonLayer(
                definition.geoJsonSource!!.invoke(),
                definition.id,
                minZoomLevel = definition.minZoomLevel,
                isTimeDependent = definition.isTimeDependent,
                refreshInterval = definition.refreshInterval,
                refreshBroadcasts = definition.refreshBroadcasts
            )

            MapLayerType.Tile -> TileMapLayer(
                definition.tileSource!!.invoke(),
                definition.id,
                shouldMultiply = definition.shouldMultiply,
                minZoomLevel = definition.minZoomLevel,
                isTimeDependent = definition.isTimeDependent,
                refreshInterval = definition.refreshInterval,
                refreshBroadcasts = definition.refreshBroadcasts,
                cacheKeys = definition.cacheKeys
            )
        }
    }

}
