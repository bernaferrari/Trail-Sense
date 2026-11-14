package com.kylecorry.sol.science.meteorology.clouds

import com.kylecorry.sol.science.meteorology.Precipitation
import com.kylecorry.sol.science.meteorology.observation.WeatherObservation
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CloudServiceCommonTest {
    @Test
    fun predictsPrecipitationRangeFromCloudTime() {
        val observed = Instant.parse("2025-01-01T00:00:00.123456789Z")
        val reading = WeatherObservation.CloudGenus(observed, CloudGenus.Cirrus).asReading()
        assertEquals(CloudGenus.Cirrus, reading.value)
        assertEquals(observed, reading.time)
        val range = CloudPrecipitationCalculator().getPrecipitationTime(listOf(reading))!!
        assertEquals(Instant.parse("2025-01-01T12:00:00.123456789Z"), range.start)
        assertEquals(Instant.parse("2025-01-02T00:00:00.123456789Z"), range.end)
    }

    @Test
    fun cloudPropertiesAndHeightDependOnLocation() {
        val clouds = CloudService()
        assertTrue(Precipitation.Lightning in clouds.getPrecipitation(CloudGenus.Cumulonimbus))
        val height = clouds.getHeightRange(CloudLevel.High, Coordinate(0.0, 0.0))
        assertEquals(6f, height.start.convertTo(DistanceUnits.Kilometers).value)
        assertEquals(18f, height.end.convertTo(DistanceUnits.Kilometers).value)
    }
}
