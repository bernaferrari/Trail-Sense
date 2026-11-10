package com.kylecorry.trail_sense.tools.tides.domain.waterlevel

import com.kylecorry.sol.math.Range
import com.kylecorry.sol.science.oceanography.waterlevel.IWaterLevelCalculator
import com.kylecorry.sol.science.oceanography.waterlevel.PiecewiseWaterLevelCalculator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant
import java.util.stream.Stream

internal class PiecewiseWaterLevelCalculatorTest {

    @ParameterizedTest
    @MethodSource("providePieces")
    fun calculate(pieces: List<Pair<Range<Instant>, IWaterLevelCalculator>>, time: Instant, expected: Float) {
        val calculator = PiecewiseWaterLevelCalculator(pieces)
        val actual = calculator.calculate(time)
        assertEquals(expected, actual, 0.01f)
    }

    companion object {
        @JvmStatic
        fun providePieces(): Stream<Arguments> {
            val one = mock<IWaterLevelCalculator>()
            whenever(one.calculate(any(), any())).thenReturn(1f)

            val two = mock<IWaterLevelCalculator>()
            whenever(two.calculate(any(), any())).thenReturn(2f)

            val three = mock<IWaterLevelCalculator>()
            whenever(three.calculate(any(), any())).thenReturn(3f)

            val base = listOf(
                Range(time(1, 1, 0), time(5, 0, 0)) to one,
                Range(time(5, 0, 0), time(10, 0, 0)) to two,
                Range(time(10, 0, 0), time(20, 0, 0)) to three
            )


            return Stream.of(
                Arguments.of(base, time(10, 0, 0), 2f),
                Arguments.of(base, time(5, 0, 0), 1f),
                Arguments.of(base, time(10, 1, 0), 3f),
                Arguments.of(base, time(20, 0, 0), 3f),
                Arguments.of(base, time(20, 1, 0), 0f),
                Arguments.of(base, time(1, 1, 0), 1f),
                Arguments.of(base, time(1, 0, 0), 0f),
                Arguments.of(emptyList<Pair<Range<Instant>, IWaterLevelCalculator>>(), time(1, 0, 0), 0f),
            )
        }

        private fun time(day: Int, hour: Int, minute: Int): Instant {
            return LocalDateTime(2022, 1, day, hour, minute).toInstant(TimeZone.UTC)
        }
    }
}
