package com.kylecorry.trail_sense.shared.map_layers.preferences.repo

import android.graphics.Bitmap
import kotlin.time.Duration

import android.content.Context
import com.kylecorry.trail_sense.shared.navigation.TrailSenseNavigator
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource

data class MapLayerDefinition(
    val id: String,
    val name: String,
    val preferences: List<MapLayerPreference> = emptyList(),
    val isConfigurable: Boolean = true,
    val layerType: MapLayerType = MapLayerType.Feature,
    val attribution: MapLayerAttribution? = null,
    val attributionLoader: (suspend (context: Context) -> MapLayerAttribution?)? = null,
    val description: String? = null,
    val minZoomLevel: Int? = null,
    val isTimeDependent: Boolean = false,
    val refreshInterval: Duration? = null,
    val refreshBroadcasts: List<String> = emptyList(),
    val cacheKeys: List<String>? = null,
    val shouldMultiply: Boolean = false,
    val geoJsonSource: (() -> GeoJsonSource)? = null,
    val tileSource: (() -> TileSource<Bitmap>)? = null,
    val openFeature: ((feature: GeoJsonFeature, context: Context, appNavigator: TrailSenseNavigator) -> Unit)? = null,
    val layer: (() -> ILayer)? = null
)
