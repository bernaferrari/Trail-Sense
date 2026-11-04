package com.kylecorry.trail_sense.settings.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class SplitReadingTest {

    @ParameterizedTest
    @MethodSource("provideReadings")
    fun splitsNumberFromUnit(text: String, number: String, unit: String?) {
        assertEquals(number to unit, splitReading(text))
    }

    companion object {
        @JvmStatic
        fun provideReadings(): List<Arguments> = listOf(
            Arguments.of("1013.2 hPa", "1013.2", "hPa"),
            Arguments.of("1,013.2 hPa", "1,013.2", "hPa"),
            Arguments.of("-12 ft", "-12", "ft"),
            Arguments.of("29.92 in", "29.92", "in"),
            Arguments.of("20 °C", "20", "°C"),
            Arguments.of("45°", "45°", null),
            Arguments.of("-", "-", null),
            Arguments.of("42.03°, -71.97°", "42.03°, -71.97°", null),
            Arguments.of("Unavailable", "Unavailable", null)
        )
    }
}
