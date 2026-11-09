package com.kylecorry.sol.science.astronomy.units

import kotlin.test.Test
import kotlin.test.assertEquals

import com.kylecorry.sol.tests.assertDate
import com.kylecorry.sol.tests.ut
import kotlinx.datetime.LocalDate
import kotlin.time.Duration.Companion.milliseconds

class GreenwichSiderealTimeTest {

    @Test
    fun toSiderealTime() {
        val gst = SiderealCalculator.at(ut(2010, 2, 7, 23, 30))
        assertEquals(8.698091, gst.hours, 0.0000005)
    }

    @Test
    fun siderealToUniversalTime() {
        val gst = GreenwichSiderealTime(8.698056)
        val actual = gst.toUniversalTime(LocalDate(2010, 2, 7))
        assertDate(ut(2010, 2, 7, 23, 30), actual, 100.milliseconds)
    }

    @Test
    fun atLongitude(){
        val gst = GreenwichSiderealTime(2.061389)
        val lst = gst.atLongitude(-40.0)
        assertEquals(23.394722, lst.hours, 0.000001)
        assertEquals(-40.0, lst.longitude, 0.000001)
    }
}