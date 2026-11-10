package com.kylecorry.sol.science.astronomy.eclipse

import kotlin.test.Test
import kotlin.test.assertTrue

import com.kylecorry.sol.science.astronomy.eclipse.lunar.LunarEclipseModel
import com.kylecorry.sol.tests.ut
import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import kotlin.time.Duration.Companion.minutes
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.tests.assertDate

class PartialLunarEclipseCalculatorTest {

    @Test
    fun skipsEclipseWhenMoonRisesAfterEclipseEnds() {
        val calculator = LunarEclipseModel(false)
        val date = ut(2030, 6, 15, 0, 0)
        val location = Coordinate(20.0, -16.0)

        var eclipse: EclipseResult? = null
        run {
            eclipse = calculator.next(date, location, TimeZone.UTC)
        }

        // The June 15 eclipse ends before the Moon rises at this location, so it must be skipped.
        assertTrue(eclipse!!.start > Instant.parse("2030-07-01T00:00:00Z"))
    }

    @Test
    fun canGetNextEclipse() {
        val calculator = LunarEclipseModel(false)
        val date = ut(2021, 8, 29, 0, 0)
        val location = Coordinate(42.0, -70.0)

        val actual = calculator.next(date, location, TimeZone.UTC)

        assertDate(
            ut(2021, 11, 19, 7, 18),
            actual!!.start,
            2.minutes
        )

        assertDate(
            ut(2021, 11, 19, 10, 47),
            actual.end,
            2.minutes
        )
    }

    @Test
    fun canGetNextEclipseOnDateOfEclipse() {
        val calculator = LunarEclipseModel(false)
        val date = ut(2021, 11, 19, 12, 0)
        val location = Coordinate(42.0, -70.0)

        val actual = calculator.next(date, location, TimeZone.UTC)

        assertDate(
            ut(2022, 5, 16, 2, 27),
            actual!!.start,
            2.minutes
        )

        assertDate(
            ut(2022, 5, 16, 5, 55),
            actual.end,
            2.minutes
        )
    }

    @Test
    fun skipsEclipseIfNotVisible() {
        val calculator = LunarEclipseModel(false)
        val date = ut(2021, 11, 1, 0, 0)
        val location = Coordinate(6.0, 22.0)

        val actual = calculator.next(date, location, TimeZone.UTC)

        assertDate(
            ut(2022, 5, 16, 2, 27),
            actual!!.start,
            2.minutes
        )

        assertDate(
            ut(2022, 5, 16, 4, 17),
            actual.end,
            2.minutes
        )
    }

}
