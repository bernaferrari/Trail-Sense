package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerDefinition
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerType
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.GeoJsonLayer

/** Builds shared feature layers and delegates platform image tile layers to the host. */
class LayerFactory<Image>(private val createTileLayer: (MapLayerDefinition<Image>) -> ILayer) {
    fun createLayer(definition: MapLayerDefinition<Image>): ILayer {
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
            MapLayerType.Tile -> createTileLayer(definition)
        }
    }
}
