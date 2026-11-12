package com.kylecorry.trail_sense.shared.navigation

import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.sol.units.Coordinate

/** Conversions between [RouteLocation] and geo URIs (intents, sharing) or coordinates (sensors). */
fun GeoUri.toRouteLocation(): RouteLocation = RouteLocation(
    latitude = coordinate.latitude,
    longitude = coordinate.longitude,
    altitude = altitude,
    parameters = queryParameters
)

fun RouteLocation.toGeoUri(): GeoUri = GeoUri(coordinate, altitude, parameters)

val RouteLocation.coordinate: Coordinate
    get() = Coordinate(latitude, longitude)

fun Coordinate.toRouteLocation(altitude: Float? = null): RouteLocation =
    RouteLocation(latitude, longitude, altitude)
