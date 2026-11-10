package com.kylecorry.sol.science.astronomy.eclipse

import kotlin.test.Test

import com.kylecorry.sol.science.astronomy.eclipse.lunar.LunarEclipseModel
import com.kylecorry.sol.tests.ut
import kotlinx.datetime.TimeZone
import kotlin.time.Duration.Companion.minutes
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.tests.assertDate

class TotalLunarEclipseCalculatorTest {

    @Test
    fun canGetNextEclipse() {
        val calculator = LunarEclipseModel(true)
        val date = ut(2021, 8, 29, 0, 0)
        val location = Coordinate(42.0, -70.0)

        val actual = calculator.next(date, location, TimeZone.UTC)

        assertDate(
            ut(2022, 5, 16, 3, 29),
            actual!!.start,
            2.minutes
        )

        assertDate(
            ut(2022, 5, 16, 4, 53),
            actual.end,
            2.minutes
        )
    }

    @Test
    fun canGetNextEclipseOnDateOfEclipse() {
        val calculator = LunarEclipseModel(true)
        val date = ut(2022, 5, 16, 5, 0)
        val location = Coordinate(42.0, -70.0)

        val actual = calculator.next(date, location, TimeZone.UTC)

        assertDate(
            ut(2022, 11, 8, 10, 16),
            actual!!.start,
            2.minutes
        )

        assertDate(
            ut(2022, 11, 8, 11, 24),
            actual.end,
            2.minutes
        )
    }

    @Test
    fun skipsEclipseIfNotVisible() {
        val calculator = LunarEclipseModel(true)
        val date = ut(2022, 5, 15, 0, 0)
        val location = Coordinate(46.0, -150.0)

        val actual = calculator.next(date, location, TimeZone.UTC)

        assertDate(
            ut(2022, 11, 8, 10, 16),
            actual!!.start,
            2.minutes
        )

        assertDate(
            ut(2022, 11, 8, 11, 41),
            actual.end,
            2.minutes
        )
    }

}