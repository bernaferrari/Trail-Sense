package com.kylecorry.trail_sense.tools.pedometer.domain

import kotlin.time.Duration.Companion.seconds
import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import com.kylecorry.sol.units.Speed
import com.kylecorry.sol.units.TimeUnits
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class StrideLengthPaceCalculatorTest {

    @ParameterizedTest
    @MethodSource("provideDistance")
    fun distance(stride: Float, steps: Long, expected: Float, units: DistanceUnits) {
        val calculator = StrideLengthPaceCalculator(Distance.from(stride, units))
        val actual = calculator.distance(steps)
        assertEquals(Distance.from(expected, units), actual)
    }

    @ParameterizedTest
    @MethodSource("provideSpeed")
    fun speed(stride: Float, steps: Long, timeSeconds: Long, expected: Float, units: DistanceUnits) {
        // kotlin.time.Duration is a value class, which JUnit cannot inject as a parameter
        val time = timeSeconds.seconds
        val calculator = StrideLengthPaceCalculator(Distance.from(stride, units))
        val actual = calculator.speed(steps, time)
        assertEquals(Speed.from(expected, units, TimeUnits.Seconds), actual)
    }


    companion object {
        @JvmStatic
        fun provideDistance(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(1f, 1000L, 1000f, DistanceUnits.Meters),
                Arguments.of(2f, 100L, 200f, DistanceUnits.Feet),
                Arguments.of(1f, 0L, 0f, DistanceUnits.Meters),
            )
        }

        @JvmStatic
        fun provideSpeed(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    1f, 1000L, 1000L, 1f,
                    DistanceUnits.Meters
                ),
                Arguments.of(
                    2f, 100L, 10L, 20f,
                    DistanceUnits.Feet
                ),
                Arguments.of(
                    1f, 0L, 100L, 0f,
                    DistanceUnits.Meters
                ),
                Arguments.of(
                    1f, 10L, 0L, 0f,
                    DistanceUnits.Meters
                ),
            )
        }
    }
}
