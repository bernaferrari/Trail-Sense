package com.kylecorry.andromeda.geojson

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance

public data class GeoJsonPosition(val x: Double, val y: Double, val z: Double? = null) {
    public val coordinate: Coordinate
        get() = Coordinate(y, x)

    public val altitude: Distance?
        get() = z?.let { Distance.meters(it.toFloat()) }
}

public interface GeoJsonGeometry : GeoJsonObject {
    public companion object {
        internal const val FIELD_COORDINATES = "coordinates"
        internal const val FIELD_BBOX = "bbox"
    }
}

public data class GeoJsonPoint(
    val point: GeoJsonPosition?,
    override val boundingBox: GeoJsonBoundingBox? = null
) :
    GeoJsonGeometry {
    override val type: String = TYPE

    public companion object {
        public const val TYPE: String = "Point"
    }
}

public data class GeoJsonLineString(
    val line: List<GeoJsonPosition>?,
    override val boundingBox: GeoJsonBoundingBox? = null
) : GeoJsonGeometry {
    override val type: String = TYPE

    public companion object {
        public const val TYPE: String = "LineString"
    }
}

public data class GeoJsonPolygon(
    val polygon: List<List<GeoJsonPosition>>?,
    override val boundingBox: GeoJsonBoundingBox? = null
) : GeoJsonGeometry {
    override val type: String = TYPE

    public fun isHole(index: Int): Boolean {
        return isHole(polygon, index)
    }

    public companion object {
        public const val TYPE: String = "Polygon"
    }
}

public data class GeoJsonMultiPoint(
    val points: List<GeoJsonPosition>?,
    override val boundingBox: GeoJsonBoundingBox? = null
) : GeoJsonGeometry {
    override val type: String = TYPE

    public companion object {
        public const val TYPE: String = "MultiPoint"
    }
}

public data class GeoJsonMultiLineString(
    val lines: List<List<GeoJsonPosition>>?,
    override val boundingBox: GeoJsonBoundingBox? = null
) : GeoJsonGeometry {
    override val type: String = TYPE

    public companion object {
        public const val TYPE: String = "MultiLineString"
    }
}

public data class GeoJsonMultiPolygon(
    val polygons: List<List<List<GeoJsonPosition>>>?,
    override val boundingBox: GeoJsonBoundingBox? = null
) : GeoJsonGeometry {
    override val type: String = TYPE

    public fun isHole(polygonIndex: Int, linearRingIndex: Int): Boolean {
        return isHole(polygons?.getOrNull(polygonIndex), linearRingIndex)
    }

    public companion object {
        public const val TYPE: String = "MultiPolygon"
    }
}

public data class GeoJsonGeometryCollection(
    val geometries: List<GeoJsonGeometry>,
    override val boundingBox: GeoJsonBoundingBox? = null
) : GeoJsonGeometry {
    override val type: String = TYPE

    public companion object {
        public const val TYPE: String = "GeometryCollection"
        internal const val FIELD_GEOMETRIES = "geometries"
    }

}

private fun isHole(polygon: List<List<GeoJsonPosition>>?, index: Int): Boolean {
    if (index == 0 || polygon == null || index > polygon.lastIndex) {
        return false
    }
    // https://stackoverflow.com/questions/1165647/how-to-determine-if-a-list-of-polygon-points-are-in-clockwise-order
    val linearRing = polygon[index]
    var signedArea = 0.0
    for (i in linearRing.indices) {
        val point1 = linearRing[i]
        val point2 = linearRing[(i + 1) % linearRing.size]
        signedArea += (point1.x * point2.y - point2.x * point1.y)
    }
    return signedArea < 0
}