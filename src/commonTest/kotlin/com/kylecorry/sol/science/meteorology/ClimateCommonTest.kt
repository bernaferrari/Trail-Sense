package com.kylecorry.sol.science.meteorology

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.Temperature
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals

class ClimateCommonTest {
    @Test
    fun classifiesTropicalRainforest() {
        val temperatures = Month.entries.associateWith { Temperature.celsius(25f) }
        val rainfall = Month.entries.associateWith { Distance.from(100f, com.kylecorry.sol.units.DistanceUnits.Millimeters) }
        assertEquals("Af", KoppenGeigerClimateClassifier.classify(temperatures, rainfall).code)
    }
}
