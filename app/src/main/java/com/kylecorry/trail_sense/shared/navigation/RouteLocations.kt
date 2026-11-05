package com.kylecorry.trail_sense.shared.navigation

import com.kylecorry.andromeda.core.system.GeoUri
import com.kylecorry.sol.units.Coordinate

/** The Android edge of [RouteLocation]: geo URIs from intents and sharing, and coordinates from sensors. */
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
