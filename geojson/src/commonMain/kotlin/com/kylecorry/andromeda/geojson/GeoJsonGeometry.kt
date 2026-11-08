package com.kylecorry.andromeda.geojson

import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance

data class GeoJsonPosition(val x: Double, val y: Double, val z: Double? = null) {
    val coordinate
        get() = Coordinate(y, x)

    val altitude
        get() = z?.let { Distance.meters(it.toFloat()) }
}

interface GeoJsonGeometry : GeoJsonObject {
    companion object {
        internal const val FIELD_COORDINATES = "coordinates"
        internal const val FIELD_BBOX = "bbox"
    }
}

data class GeoJsonPoint(
    val point: GeoJsonPosition?,
    override val boundingBox: GeoJsonBoundingBox? = null
) :
    GeoJsonGeometry {
    override val type = TYPE

    companion object {
        const val TYPE = "Point"
    }
}

data class GeoJsonLineString(
    val line: List<GeoJsonPosition>?,
    override val boundingBox: GeoJsonBoundingBox? = null
) : GeoJsonGeometry {
    override val type = TYPE

    companion object {
        const val TYPE = "LineString"
    }
}

data class GeoJsonPolygon(
    val polygon: List<List<GeoJsonPosition>>?,
    override val boundingBox: GeoJsonBoundingBox? = null
) : GeoJsonGeometry {
    override val type = TYPE

    fun isHole(index: Int): Boolean {
        return isHole(polygon, index)
    }

    companion object {
        const val TYPE = "Polygon"
    }
}

data class GeoJsonMultiPoint(
    val points: List<GeoJsonPosition>?,
    override val boundingBox: GeoJsonBoundingBox? = null
) : GeoJsonGeometry {
    override val type = TYPE

    companion object {
        const val TYPE = "MultiPoint"
    }
}

data class GeoJsonMultiLineString(
    val lines: List<List<GeoJsonPosition>>?,
    override val boundingBox: GeoJsonBoundingBox? = null
) : GeoJsonGeometry {
    override val type = TYPE

    companion object {
        const val TYPE = "MultiLineString"
    }
}

data class GeoJsonMultiPolygon(
    val polygons: List<List<List<GeoJsonPosition>>>?,
    override val boundingBox: GeoJsonBoundingBox? = null
) : GeoJsonGeometry {
    override val type = TYPE

    fun isHole(polygonIndex: Int, linearRingIndex: Int): Boolean {
        return isHole(polygons?.getOrNull(polygonIndex), linearRingIndex)
    }

    companion object {
        const val TYPE = "MultiPolygon"
    }
}

data class GeoJsonGeometryCollection(
    val geometries: List<GeoJsonGeometry>,
    override val boundingBox: GeoJsonBoundingBox? = null
) : GeoJsonGeometry {
    override val type = TYPE

    companion object {
        const val TYPE = "GeometryCollection"
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