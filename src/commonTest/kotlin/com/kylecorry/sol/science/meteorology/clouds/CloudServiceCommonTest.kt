package com.kylecorry.sol.science.meteorology.clouds

import com.kylecorry.sol.science.meteorology.Precipitation
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.DistanceUnits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CloudServiceCommonTest {
    @Test
    fun cloudPropertiesAndHeightDependOnLocation() {
        val clouds = CloudService()
        assertTrue(Precipitation.Lightning in clouds.getPrecipitation(CloudGenus.Cumulonimbus))
        val height = clouds.getHeightRange(CloudLevel.High, Coordinate(0.0, 0.0))
        assertEquals(6f, height.start.convertTo(DistanceUnits.Kilometers).value)
        assertEquals(18f, height.end.convertTo(DistanceUnits.Kilometers).value)
    }
}
