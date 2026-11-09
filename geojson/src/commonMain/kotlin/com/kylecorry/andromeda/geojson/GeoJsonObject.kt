package com.kylecorry.andromeda.geojson

public interface GeoJsonObject {
    public val type: String
    public val boundingBox: GeoJsonBoundingBox?
}