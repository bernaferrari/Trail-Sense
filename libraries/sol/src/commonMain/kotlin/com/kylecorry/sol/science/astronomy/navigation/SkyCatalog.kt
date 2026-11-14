package com.kylecorry.sol.science.astronomy.navigation

import com.kylecorry.sol.math.Vector3Precise
import com.kylecorry.sol.science.astronomy.AstroUtils
import com.kylecorry.sol.science.astronomy.locators.StarPositionModel
import com.kylecorry.sol.science.astronomy.locators.PlanetPositionModel
import com.kylecorry.sol.science.astronomy.locators.Planet
import com.kylecorry.sol.science.astronomy.stars.STAR_CATALOG
import com.kylecorry.sol.units.Coordinate
import kotlin.time.Instant
import kotlin.math.PI
import kotlin.math.sin

/**
 * A celestial body's apparent direction as an East-North-Up unit vector
 */

internal object SkyCatalog {

    private val planetMagnitudes = mapOf(
        Planet.Mercury to 0f,
        Planet.Venus to -4f,
        Planet.Mars to 0.5f,
        Planet.Jupiter to -2f,
        Planet.Saturn to 0.7f
    )

    fun getBodies(maxMagnitude: Float, includePlanets: Boolean = true): List<CelestialBody> {
        val stars = STAR_CATALOG.filter { it.magnitude <= maxMagnitude }
            .map { CelestialBody(it.name, it.magnitude, it, null) }
        val planets = if (includePlanets) {
            planetMagnitudes.map { CelestialBody(it.key.name, it.value, null, it.key) }
        } else {
            emptyList()
        }
        return stars + planets
    }

    fun getPosition(
        body: CelestialBody,
        time: Instant,
        location: Coordinate,
        withRefraction: Boolean = true
    ): BodyPosition {
        val observation = if (body.star != null) {
            AstroUtils.getLocation(StarPositionModel(body.star), time, location, withRefraction)
        } else {
            AstroUtils.getLocation(PlanetPositionModel(checkNotNull(body.planet)), time, location, withRefraction)
        }
        return BodyPosition(
            body,
            HorizonVectors.toVector(observation.azimuth.toFloat().toDouble(), observation.altitude.toFloat().toDouble())
        )
    }

    fun getPositions(
        bodies: List<CelestialBody>,
        time: Instant,
        location: Coordinate,
        withRefraction: Boolean = true,
        minAltitude: Double = -90.0
    ): List<BodyPosition> {
        val minZ = sin(minAltitude * PI / 180)
        return bodies.map { getPosition(it, time, location, withRefraction) }.filter { it.vector.z >= minZ }
    }
}
