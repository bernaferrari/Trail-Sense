package com.kylecorry.sol.units

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

import assertk.assertThat
import assertk.assertions.isEqualTo

class CoordinateTest {

    @Test
    fun canRestrictLongitude(){
        assertThat(Coordinate.toLongitude(10.0)).isEqualTo(10.0)
        assertThat(Coordinate.toLongitude(-180.0)).isEqualTo(-180.0)
        assertThat(Coordinate.toLongitude(180.0)).isEqualTo(180.0)
        assertThat(Coordinate.toLongitude(200.0)).isEqualTo(-160.0)
        assertThat(Coordinate.toLongitude(-200.0)).isEqualTo(160.0)
        assertThat(Coordinate.toLongitude(570.0)).isEqualTo(-150.0)
        assertThat(Coordinate.toLongitude(-660.0)).isEqualTo(60.0)
    }

    @Test
    fun isNorthernHemisphere() {
        assertTrue(Coordinate(1.0, 0.0).isNorthernHemisphere)
        assertFalse(Coordinate(-1.0, 0.0).isNorthernHemisphere)
        assertTrue(Coordinate(0.0, 0.0).isNorthernHemisphere)
    }

    @Test
    fun constructorConstrainsAndRoundTrips() {
        constructorConstrainsAndRoundTrips(100.0, 200.0, 90.0, -160.0)
        constructorConstrainsAndRoundTrips(-100.0, -200.0, -90.0, 160.0)
        constructorConstrainsAndRoundTrips(42.815, -70.8733, 42.815, -70.8733)
        constructorConstrainsAndRoundTrips(0.0, 0.0, 0.0, 0.0)
        constructorConstrainsAndRoundTrips(90.0, 180.0, 90.0, 180.0)
        constructorConstrainsAndRoundTrips(-90.0, -180.0, -90.0, -180.0)
        constructorConstrainsAndRoundTrips(89.9999999, 179.9999999, 89.9999999, 179.9999999)
        constructorConstrainsAndRoundTrips(-89.9999999, -179.9999999, -89.9999999, -179.9999999)
        constructorConstrainsAndRoundTrips(45.123456789, -179.999999999, 45.123456789, -179.999999999)
        constructorConstrainsAndRoundTrips(-45.987654321, 179.999999999, -45.987654321, 179.999999999)
        constructorConstrainsAndRoundTrips(12.345678901, 540.0, 12.345678901, -180.0)
        constructorConstrainsAndRoundTrips(12.345678901, -540.0, 12.345678901, 180.0)
        constructorConstrainsAndRoundTrips(12.345678901, 539.999999999, 12.345678901, 179.999999999)
        constructorConstrainsAndRoundTrips(12.345678901, -539.999999999, 12.345678901, -179.999999999)
    }

    private fun constructorConstrainsAndRoundTrips(
        latitude: Double,
        longitude: Double,
        expectedLatitude: Double,
        expectedLongitude: Double
    ) {
        val coordinate = Coordinate(latitude, longitude)

        assertEquals(expectedLatitude, coordinate.latitude, 0.000001)
        assertEquals(expectedLongitude, coordinate.longitude, 0.000001)
    }

    @Test
    fun canConvertDoubleCoordinate() {
        val coordinate = DoubleCoordinate(100.0, 200.0).toCoordinate()

        assertEquals(90.0, coordinate.latitude, 0.000001)
        assertEquals(-160.0, coordinate.longitude, 0.000001)
    }

    @Test
    fun canAddDistance() {
        val start = Coordinate(40.0, 10.0)
        val bearing = Bearing.from(100f)
        val distance = 10000.0

        val expected = Coordinate(39.984444, 10.115556)
        val actual = start.plus(distance, bearing)
        assertEquals(expected.latitude, actual.latitude, 0.01)
        assertEquals(expected.longitude, actual.longitude, 0.01)
    }

    @Test
    fun degreesBetween() {
        // Identical coordinates
        degreesBetween(0.0, 0.0, 0.0, 0.0, 0.0f)
        degreesBetween(42.0, -70.0, 42.0, -70.0, 0.0f)
        // Along the equator
        degreesBetween(0.0, 0.0, 0.0, 45.0, 45.0f)
        degreesBetween(0.0, 0.0, 0.0, 90.0, 90.0f)
        degreesBetween(0.0, 0.0, 0.0, 180.0, 180.0f)
        // Along a meridian
        degreesBetween(0.0, 0.0, 45.0, 0.0, 45.0f)
        degreesBetween(-45.0, 0.0, 0.0, 0.0, 45.0f)
        degreesBetween(0.0, 0.0, -90.0, 0.0, 90.0f)
        // Poles
        degreesBetween(90.0, 0.0, -90.0, 0.0, 180.0f)
        degreesBetween(90.0, 0.0, 0.0, 45.0, 90.0f)
        degreesBetween(90.0, 0.0, 90.0, 135.0, 0.0f)
        // Across the antimeridian
        degreesBetween(0.0, 179.0, 0.0, -179.0, 2.0f)
        // Antipodal points
        degreesBetween(45.0, 10.0, -45.0, -170.0, 180.0f)
        // Same latitude, different longitude
        degreesBetween(60.0, 0.0, 60.0, 90.0, 41.40962f)
        // Arbitrary coordinates (and symmetry)
        degreesBetween(60.0, 30.0, -30.0, 100.0, 106.55474f)
        degreesBetween(-30.0, 100.0, 60.0, 30.0, 106.55474f)
        // Small separation
        degreesBetween(0.0, 0.0, 0.0, 0.5, 0.5f)
    }

    private fun degreesBetween(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double,
        expected: Float
    ) {
        val start = Coordinate(latitude1, longitude1)
        val end = Coordinate(latitude2, longitude2)

        val degrees = start.degreesBetween(end)

        assertEquals(expected, degrees, 0.01f)
    }

}
