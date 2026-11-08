package com.kylecorry.andromeda.geojson


data class GeoJsonFeatureCollection(
    val features: List<GeoJsonFeature>,
    override val boundingBox: GeoJsonBoundingBox? = null
) : GeoJsonObject {
    override val type = TYPE

    companion object {
        internal const val TYPE = "FeatureCollection"
        internal const val FIELD_FEATURES = "features"
        internal const val FIELD_BBOX = "bbox"
    }
}
