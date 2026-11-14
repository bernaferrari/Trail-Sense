package com.kylecorry.sol.science.geophysics

import kotlin.test.Test
import kotlin.test.assertEquals

import assertk.assertThat
import assertk.assertions.isCloseTo
import com.kylecorry.sol.units.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

internal class GeophysicsTest {

    @Test
    fun gravity() {
        assertThat(Geophysics.getGravity(Coordinate.zero)).isCloseTo(9.78032677f, 0.00001f)
        assertThat(Geophysics.getGravity(Coordinate(90.0, 0.0))).isCloseTo(
            9.83218493786340046183f,
            0.00001f
        )
        assertThat(Geophysics.getGravity(Coordinate(-90.0, 0.0))).isCloseTo(
            9.83218493786340046183f,
            0.00001f
        )
    }

    @Test
    fun getGeomagneticDeclination() {
        getGeomagneticDeclination("2025-01-01", 80.0, 0.0, 0.0f, 1.28f)
        getGeomagneticDeclination("2025-01-01", 0.0, 120.0, 0.0f, -0.16f)
        getGeomagneticDeclination("2025-01-01", -80.0, 240.0, 0.0f, 68.78f)
        getGeomagneticDeclination("2025-01-01", 80.0, 0.0, 100000.0f, 0.85f)
        getGeomagneticDeclination("2025-01-01", 0.0, 120.0, 100000.0f, -0.15f)
        getGeomagneticDeclination("2025-01-01", -80.0, 240.0, 100000.0f, 68.21f)
        getGeomagneticDeclination("2027-07-01", 80.0, 0.0, 0.0f, 2.59f)
        getGeomagneticDeclination("2027-07-01", 0.0, 120.0, 0.0f, -0.24f)
        getGeomagneticDeclination("2027-07-01", -80.0, 240.0, 0.0f, 68.49f)
        getGeomagneticDeclination("2027-07-01", 80.0, 0.0, 100000.0f, 2.16f)
        getGeomagneticDeclination("2027-07-01", 0.0, 120.0, 100000.0f, -0.23f)
        getGeomagneticDeclination("2027-07-01", -80.0, 240.0, 100000.0f, 67.93f)
    }

    private fun getGeomagneticDeclination(dateStr: String, lat: Double, lon: Double, altitude: Float, expected: Float) {
        val location = Coordinate(lat, lon)
        val actual = Geophysics.getGeomagneticDeclination(
            location,
            altitude,
            LocalDate.parse(dateStr).atStartOfDayIn(TimeZone.UTC)
        )
        assertEquals(expected, actual, 0.01f)
    }

    @Test
    fun getGeomagneticInclination() {
        getGeomagneticInclination("2025-01-01", 80.0, 0.0, 0.0f, 83.21f)
        getGeomagneticInclination("2025-01-01", 0.0, 120.0, 0.0f, -14.93f)
        getGeomagneticInclination("2025-01-01", -80.0, 240.0, 0.0f, -72.0f)
        getGeomagneticInclination("2025-01-01", 80.0, 0.0, 100000.0f, 83.26f)
        getGeomagneticInclination("2025-01-01", 0.0, 120.0, 100000.0f, -15.08f)
        getGeomagneticInclination("2025-01-01", -80.0, 240.0, 100000.0f, -72.19f)
        getGeomagneticInclination("2027-07-01", 80.0, 0.0, 0.0f, 83.24f)
        getGeomagneticInclination("2027-07-01", 0.0, 120.0, 0.0f, -14.65f)
        getGeomagneticInclination("2027-07-01", -80.0, 240.0, 0.0f, -71.92f)
        getGeomagneticInclination("2027-07-01", 80.0, 0.0, 100000.0f, 83.29f)
        getGeomagneticInclination("2027-07-01", 0.0, 120.0, 100000.0f, -14.81f)
        getGeomagneticInclination("2027-07-01", -80.0, 240.0, 100000.0f, -72.1f)
    }

    private fun getGeomagneticInclination(dateStr: String, lat: Double, lon: Double, altitude: Float, expected: Float) {
        val location = Coordinate(lat, lon)
        val actual = Geophysics.getGeomagneticInclination(
            location,
            altitude,
            LocalDate.parse(dateStr).atStartOfDayIn(TimeZone.UTC)
        )
        assertEquals(expected, actual, 0.01f)
    }

}
