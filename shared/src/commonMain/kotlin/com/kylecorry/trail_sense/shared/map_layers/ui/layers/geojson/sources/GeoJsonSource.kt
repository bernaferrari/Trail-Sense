package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerValues
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds

interface GeoJsonSource {
    suspend fun load(
        bounds: CoordinateBounds,
        zoom: Int,
        params: MapLayerValues = MapLayerValues.Empty
    ): GeoJsonObject?

    suspend fun cleanup() {
        // Do nothing
    }
}
