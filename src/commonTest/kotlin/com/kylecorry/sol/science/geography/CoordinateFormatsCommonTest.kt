package com.kylecorry.sol.science.geography

import com.kylecorry.sol.science.geography.formatting.DecimalDegreesCoordinateFormat
import com.kylecorry.sol.science.geography.formatting.MGRSCoordinateFormat
import com.kylecorry.sol.science.geography.formatting.OSGBCoordinateFormat
import com.kylecorry.sol.units.Coordinate
import kotlin.test.Test
import kotlin.test.assertEquals

class CoordinateFormatsCommonTest {
    @Test
    fun formatCoordinates() {
        val decimalFormat = DecimalDegreesCoordinateFormat(4)
        val coordinate = Coordinate(42.1948, -71.6295)
        assertEquals(coordinate, decimalFormat.parse(decimalFormat.toString(coordinate)))
        assertEquals("27PXM 09601 05579", MGRSCoordinateFormat(5).toString(Coordinate(10.0, -20.0)))
        assertEquals("TG 51409 13177", OSGBCoordinateFormat().toString(Coordinate(52.657977, 1.716038)))
    }
}
