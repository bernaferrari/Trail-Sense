package com.kylecorry.andromeda.geojson


public data class GeoJsonFeature(
    val id: Any?,
    val geometry: GeoJsonGeometry?,
    val properties: Map<String, Any?>? = null,
    override val boundingBox: GeoJsonBoundingBox? = null
) : GeoJsonObject {
    override val type: String = TYPE

    public companion object {
        internal const val TYPE = "Feature"
        internal const val FIELD_ID = "id"
        internal const val FIELD_GEOMETRY = "geometry"
        internal const val FIELD_PROPERTIES = "properties"
        internal const val FIELD_BBOX = "bbox"
    }
}
