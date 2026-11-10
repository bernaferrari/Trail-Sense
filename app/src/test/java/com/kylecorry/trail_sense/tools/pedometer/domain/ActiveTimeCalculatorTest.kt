package com.kylecorry.trail_sense.tools.pedometer.domain

import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class ActiveTimeCalculatorTest {

    private val calculator = ActiveTimeCalculator()

    @ParameterizedTest
    @MethodSource("provideActiveTimes")
    fun calculate(steps: Long, elapsedSeconds: Long, expectedSeconds: Long) {
        // kotlin.time.Duration is a value class, which JUnit cannot inject as a parameter
        assertEquals(expectedSeconds.seconds, calculator.calculate(steps, elapsedSeconds.seconds))
    }

    companion object {
        @JvmStatic
        fun provideActiveTimes(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(0L, 10L, 0L),
                Arguments.of(-1L, 10L, 0L),
                Arguments.of(10L, 0L, 0L),
                Arguments.of(10L, -1L, 0L),
                Arguments.of(10L, 10L, 10L),
                Arguments.of(10L, 60L, 20L),
                Arguments.of(10L, 15L, 15L)
            )
        }
    }
}
