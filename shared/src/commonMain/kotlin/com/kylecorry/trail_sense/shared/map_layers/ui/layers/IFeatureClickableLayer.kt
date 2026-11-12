package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import com.kylecorry.andromeda.geojson.GeoJsonFeature

typealias OnGeoJsonFeatureClickListener = (GeoJsonFeature) -> Unit

/** A layer whose GeoJSON features can be tapped. */
interface IFeatureClickableLayer {
    fun setOnFeatureClickListener(listener: OnGeoJsonFeatureClickListener?)
}
