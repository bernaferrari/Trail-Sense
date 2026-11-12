package com.kylecorry.trail_sense.tools.climate.infrastructure

import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertTrue

class MonthlyValueInterpolatorTest {

    @Test
    fun canInterpolateFromDecemberTowardJanuary() {
        val monthlyValues = Month.entries.associateWith { (it.ordinal + 1).toFloat() }
        val interpolator = MonthlyValueInterpolator()

        val decemberValue = interpolator.interpolate(LocalDate(2025, Month.DECEMBER, 15), monthlyValues)
        val lateDecemberValue = interpolator.interpolate(LocalDate(2025, Month.DECEMBER, 16), monthlyValues)

        assertTrue(
            lateDecemberValue < decemberValue,
            "December 16 should move from December's value toward January's lower value"
        )
    }
}
