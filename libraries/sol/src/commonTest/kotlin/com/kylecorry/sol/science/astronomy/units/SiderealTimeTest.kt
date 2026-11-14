package com.kylecorry.sol.science.astronomy.units

import kotlin.test.Test
import kotlin.test.assertEquals

class SiderealTimeTest {

    @Test
    fun toGreenwich() {
        val lst = SiderealTime(23.394722, 50.0)
        val gst = lst.atGreenwich()
        assertEquals(20.061389, gst.hours, 0.000001)
    }

}