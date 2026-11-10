package com.kylecorry.trail_sense.tools.waterpurification.domain

import com.kylecorry.sol.units.Distance
import com.kylecorry.sol.units.DistanceUnits
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.time.Duration.Companion.minutes
import java.util.stream.Stream

internal class WaterServiceTest {

    @ParameterizedTest
    @MethodSource("providePurificationTimes")
    fun getPurificationTime(altitude: Distance?, minutes: Long) {
        // kotlin.time.Duration is a value class, which JUnit cannot inject as a parameter
        val duration = minutes.minutes
        val service = WaterService()
        val time = service.getPurificationTime(altitude)
        assertEquals(time, duration)
    }

    companion object {
        @JvmStatic
        fun providePurificationTimes(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(Distance.from(0f, DistanceUnits.Meters), 1L),
                Arguments.of(Distance.from(999f, DistanceUnits.Meters), 1L),
                Arguments.of(Distance.from(1000f, DistanceUnits.Meters), 3L),
                Arguments.of(Distance.from(2000f, DistanceUnits.Meters), 3L),
                Arguments.of(null, 3L),
            )
        }
    }
}
