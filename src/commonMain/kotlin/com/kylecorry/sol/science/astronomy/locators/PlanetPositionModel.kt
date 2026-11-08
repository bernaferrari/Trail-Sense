package com.kylecorry.sol.science.astronomy.locators

import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.arithmetic.Arithmetic.square
import com.kylecorry.sol.math.trigonometry.Trigonometry.cosDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.normalizeAngle
import com.kylecorry.sol.math.trigonometry.Trigonometry.sinDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry.tanDegrees
import com.kylecorry.sol.science.astronomy.OrbitalMath
import com.kylecorry.sol.science.astronomy.units.EclipticCoordinate
import com.kylecorry.sol.science.astronomy.units.EquatorialCoordinate
import com.kylecorry.sol.science.astronomy.units.JulianDay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import com.kylecorry.sol.units.Distance
import kotlin.math.atan2
import kotlin.math.log10
import kotlin.math.sqrt

// Equations from Celestial Calculations by J. L. Lawrence chapter 8
internal class PlanetPositionModel(private val planet: Planet) {
    fun getCoordinates(ut: Instant): EquatorialCoordinate {
        val planetPosition = getHeliocentricPosition(planet, ut)
        val earthPosition = getHeliocentricPosition(Planet.Earth, ut)
        val pLong = planetPosition.first.eclipticLongitude
        val pLat = planetPosition.first.eclipticLatitude
        val eLong = earthPosition.first.eclipticLongitude
        val longDiffNode = pLong - planet.eclipticLongitudeAscendingNodeEpoch
        val longitudeAdjustment = normalizeAngle(
            planet.eclipticLongitudeAscendingNodeEpoch + atan2(
                sinDegrees(longDiffNode) * cosDegrees(planet.inclination),
                cosDegrees(longDiffNode)
            ).toDegrees()
        )
        val pCosLat = planetPosition.second * cosDegrees(pLat)
        val longDiffEarth = eLong - longitudeAdjustment
        val geocentricEclipticLongitude = normalizeAngle(
            if (planet.order <= Planet.Earth.order) {
                180 + eLong + atan2(
                    pCosLat * sinDegrees(longDiffEarth),
                    earthPosition.second - pCosLat * cosDegrees(longDiffEarth)
                ).toDegrees()
            } else {
                longitudeAdjustment + atan2(
                    earthPosition.second * sinDegrees(longitudeAdjustment - eLong),
                    pCosLat - earthPosition.second * cosDegrees(longDiffEarth)
                ).toDegrees()
            }
        )
        val geocentricEclipticLatitude = normalizeAngle(
            atan2(
                pCosLat * tanDegrees(pLat) *
                    sinDegrees(geocentricEclipticLongitude - longitudeAdjustment),
                earthPosition.second * sinDegrees(longitudeAdjustment - eLong)
            ).toDegrees()
        )
        val coordinate = EclipticCoordinate(geocentricEclipticLatitude, geocentricEclipticLongitude)
        return coordinate.toEquatorial(EclipticCoordinate.getObliquityOfTheEcliptic(JulianDay.centuries(ut)))
    }

    fun getDistance(ut: Instant): Distance {
        return Distance.kilometers(
            (149597871.0 * getDistanceAU(
                getHeliocentricPosition(planet, ut),
                getHeliocentricPosition(Planet.Earth, ut)
            )).toFloat()
        )
    }

    fun getAngularDiameter(ut: Instant): Double {
        val distance = getDistanceAU(getHeliocentricPosition(planet, ut), getHeliocentricPosition(Planet.Earth, ut))
        return planet.angularDiameter / distance
    }

    fun getMagnitude(ut: Instant): Double {
        val planetPosition = getHeliocentricPosition(planet, ut)
        val earthPosition = getHeliocentricPosition(Planet.Earth, ut)
        val geocentricPlanetPosition = getGeocentricPosition(planetPosition, earthPosition)
        val distance = getDistanceAU(planetPosition, earthPosition)
        val phaseAngle =
            (1 + cosDegrees(geocentricPlanetPosition.eclipticLongitude - planetPosition.first.eclipticLongitude)) / 2
        return planet.visualMagnitude + 5 * log10((planetPosition.second * distance) / sqrt(phaseAngle))
    }

    private fun getGeocentricPosition(
        planetPosition: Pair<EclipticCoordinate, Double>,
        earthPosition: Pair<EclipticCoordinate, Double>
    ): EclipticCoordinate {
        val pLong = planetPosition.first.eclipticLongitude
        val pLat = planetPosition.first.eclipticLatitude
        val eLong = earthPosition.first.eclipticLongitude
        val longDiffNode = pLong - planet.eclipticLongitudeAscendingNodeEpoch
        val longitudeAdjustment = normalizeAngle(
            planet.eclipticLongitudeAscendingNodeEpoch + atan2(
                sinDegrees(longDiffNode) * cosDegrees(planet.inclination),
                cosDegrees(longDiffNode)
            ).toDegrees()
        )
        val pCosLat = planetPosition.second * cosDegrees(pLat)
        val longDiffEarth = eLong - longitudeAdjustment
        val geocentricEclipticLongitude = normalizeAngle(
            if (planet.order <= Planet.Earth.order) {
                180 + eLong + atan2(
                    pCosLat * sinDegrees(longDiffEarth),
                    earthPosition.second - pCosLat * cosDegrees(longDiffEarth)
                ).toDegrees()
            } else {
                longitudeAdjustment + atan2(
                    earthPosition.second * sinDegrees(longitudeAdjustment - eLong),
                    pCosLat - earthPosition.second * cosDegrees(longDiffEarth)
                ).toDegrees()
            }
        )
        val geocentricEclipticLatitude = normalizeAngle(
            atan2(
                pCosLat * tanDegrees(pLat) *
                    sinDegrees(geocentricEclipticLongitude - longitudeAdjustment),
                earthPosition.second * sinDegrees(longitudeAdjustment - eLong)
            ).toDegrees()
        )
        return EclipticCoordinate(geocentricEclipticLatitude, geocentricEclipticLongitude)
    }

    private fun getDistanceAU(
        planetPosition: Pair<EclipticCoordinate, Double>,
        earthPosition: Pair<EclipticCoordinate, Double>
    ): Double {
        val longDiff = planetPosition.first.eclipticLongitude - earthPosition.first.eclipticLongitude
        return sqrt(
            square(earthPosition.second) + square(planetPosition.second) -
                2 * earthPosition.second * planetPosition.second * cosDegrees(longDiff)
        )
    }

    private fun getHeliocentricPosition(planet: Planet, ut: Instant): Pair<EclipticCoordinate, Double> {
        val meanAnomaly = OrbitalMath.getMeanAnomaly(
            JulianDay.of(ut.toLocalDateTime(TimeZone.UTC)) - planet.epoch,
            OrbitalMath.EARTH_ORBITAL_PERIOD_DAYS * planet.orbitalPeriod,
            planet.eclipticLongitudeEpoch,
            planet.eclipticLongitudePerihelion
        )
        val trueAnomaly = OrbitalMath.estimateTrueAnomaly(planet.eccentricity, meanAnomaly)
        val coordinate = OrbitalMath.getEclipticCoordinate(
            trueAnomaly,
            planet.eclipticLongitudePerihelion,
            planet.eclipticLongitudeAscendingNodeEpoch,
            planet.inclination
        )
        val radius = OrbitalMath.getRadius(trueAnomaly, planet.semimajorAxis, planet.eccentricity)
        return Pair(coordinate, radius)
    }
}
