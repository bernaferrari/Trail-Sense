package com.kylecorry.sol.units

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

internal class SpeedTest {

    @Test
    fun convertTo() {
        provideSpeedConversions().forEach { args -> convertTo(args[0].arg<Float>(), args[1].arg<DistanceUnits>(), args[2].arg<TimeUnits>(), args[3].arg<Float>(), args[4].arg<DistanceUnits>(), args[5].arg<TimeUnits>()) }
    }

    private fun convertTo(
        speed: Float,
        distanceUnits: DistanceUnits,
        timeUnits: TimeUnits,
        expected: Float,
        expectedDistanceUnits: DistanceUnits,
        expectedTimeUnits: TimeUnits
    ) {
        assertEquals(
            Speed.from(expected, expectedDistanceUnits, expectedTimeUnits),
            Speed.from(speed, distanceUnits, timeUnits).convertTo(expectedDistanceUnits, expectedTimeUnits)
        )
    }

    companion object {
        fun provideSpeedConversions(): List<List<Any?>> {
            return listOf(
                listOf(1f, DistanceUnits.Meters, TimeUnits.Seconds, 1f, DistanceUnits.Meters, TimeUnits.Seconds),
                listOf(
                    1f,
                    DistanceUnits.Meters,
                    TimeUnits.Seconds,
                    0.001f,
                    DistanceUnits.Meters,
                    TimeUnits.Milliseconds
                ),
                listOf(1f, DistanceUnits.Meters, TimeUnits.Seconds, 60f, DistanceUnits.Meters, TimeUnits.Minutes),
                listOf(1f, DistanceUnits.Meters, TimeUnits.Seconds, 3600f, DistanceUnits.Meters, TimeUnits.Hours),
                listOf(1f, DistanceUnits.Meters, TimeUnits.Seconds, 86400f, DistanceUnits.Meters, TimeUnits.Days),
                listOf(
                    5f,
                    DistanceUnits.Feet,
                    TimeUnits.Hours,
                    3657.5999f,
                    DistanceUnits.Centimeters,
                    TimeUnits.Days
                ),
                listOf(
                    86400f,
                    DistanceUnits.Centimeters,
                    TimeUnits.Days,
                    0.01f,
                    DistanceUnits.Meters,
                    TimeUnits.Seconds
                )
            )
        }

    }

}