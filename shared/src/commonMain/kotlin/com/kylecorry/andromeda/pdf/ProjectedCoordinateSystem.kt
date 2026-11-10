package com.kylecorry.andromeda.pdf

public data class ProjectedCoordinateSystem(
    val geographic: GeographicCoordinateSystem,
    val projection: String
)

public data class GeographicCoordinateSystem(val datum: Datum, val primeMeridian: Double)

public data class Datum(val name: String, val spheroid: Spheroid)

public data class Spheroid(val name: String, val semiMajorAxis: Float, val inverseFlattening: Float)