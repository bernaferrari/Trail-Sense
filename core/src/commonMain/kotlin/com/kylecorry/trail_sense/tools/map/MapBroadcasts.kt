package com.kylecorry.trail_sense.tools.map

/** The broadcasts the map tool sends when a GeoJSON feature is selected or deselected. */
object MapBroadcasts {
    const val GEOJSON_FEATURE_SELECTION_CHANGED = "map-broadcast-geojson-feature-selection-changed"
    const val PARAM_GEOJSON_FEATURE_ID = "featureId"
    const val PARAM_GEOJSON_LAYER_ID = "layerId"
}
