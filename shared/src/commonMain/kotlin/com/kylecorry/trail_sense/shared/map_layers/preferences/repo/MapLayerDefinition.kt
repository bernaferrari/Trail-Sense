package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

import kotlin.time.Duration

import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource

data class MapLayerDefinition<Image>(
    val id: String,
    val name: String,
    val preferences: List<MapLayerPreference> = emptyList(),
    val isConfigurable: Boolean = true,
    val layerType: MapLayerType = MapLayerType.Feature,
    val attribution: MapLayerAttribution? = null,
    val attributionLoader: (suspend () -> MapLayerAttribution?)? = null,
    val description: String? = null,
    val minZoomLevel: Int? = null,
    val isTimeDependent: Boolean = false,
    val refreshInterval: Duration? = null,
    val refreshBroadcasts: List<String> = emptyList(),
    val cacheKeys: List<String>? = null,
    val shouldMultiply: Boolean = false,
    val geoJsonSource: (() -> GeoJsonSource)? = null,
    val tileSource: (() -> TileSource<Image>)? = null,
    val openFeature: ((feature: GeoJsonFeature, appNavigator: TrailSenseNavigator) -> Unit)? = null,
    val layer: (() -> ILayer)? = null
)
