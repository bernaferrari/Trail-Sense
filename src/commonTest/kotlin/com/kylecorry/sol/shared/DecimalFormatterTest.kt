package com.kylecorry.sol.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

internal class DecimalFormatterTest {

    @Test
    fun format() {
        provideValues().forEach { args -> format(args[0].arg<Number>(), args[1].arg<Int>(), args[2].arg<Boolean>(), args[3].arg<String>()) }
    }

    private fun format(value: Number, places: Int, strict: Boolean, expected: String) {
        assertEquals(expected, DecimalFormatter.format(value, places, strict, NumberLocale.Invariant))
    }

    @Test
    fun formatUsesTheDecimalSeparatorOfTheLocale() {
        val brazil = NumberLocale.Tag("pt-BR")
        assertEquals("1,2", DecimalFormatter.format(1.155, 1, locale = brazil))
        assertEquals("-1,2", DecimalFormatter.format(-1.155, 1, locale = brazil))
        assertEquals("100", DecimalFormatter.format(100, 2, strict = false, locale = brazil))
        assertEquals("0,00000010", DecimalFormatter.format(1.0E-7, 8, locale = brazil))
        assertEquals("-", DecimalFormatter.format(Double.NaN, 2, locale = brazil))
        assertEquals("1.2", DecimalFormatter.format(1.155, 1, locale = NumberLocale.Tag("en-US")))
    }

    @Test
    fun formatDoesNotGroupDigits() {
        assertEquals("1234567,5", DecimalFormatter.format(1234567.5, 1, locale = NumberLocale.Tag("de-DE")))
        assertEquals("1234567.5", DecimalFormatter.format(1234567.5, 1, locale = NumberLocale.Tag("en-US")))
    }

    @Test
    fun formatUsesTheSystemLocaleByDefault() {
        val symbols = platformDecimalSymbols(languageTag = null)
        val expected = "1${symbols.decimalSeparator}5".map {
            if (it.isDigit()) symbols.zeroDigit + (it - '0') else it
        }.joinToString("")
        assertEquals(expected, DecimalFormatter.format(1.5, 1))
        assertEquals(expected, DecimalFormatter.format(1.5, 1, locale = NumberLocale.System))
    }

    @Test
    fun invariantIgnoresTheLocale() {
        assertEquals("-1234567.5", DecimalFormatter.format(-1234567.5, 1, locale = NumberLocale.Invariant))
    }

    companion object {
        fun provideValues(): List<List<Any?>> {
            return listOf(
                listOf(1.155, 1, false, "1.2"),
                listOf(1.155, 1, true, "1.2"),
                listOf(1.155, 4, false, "1.155"),
                listOf(1.155, 4, true, "1.1550"),
                listOf(1.155, 0, true, "1"),
                listOf(0.155, 2, true, "0.16"),
                listOf(100.155, 2, true, "100.16"),
                listOf(100, 2, true, "100.00"),
                listOf(100, 2, false, "100"),
                listOf(Double.NaN, 2, true, "-"),
                listOf(Double.NEGATIVE_INFINITY, 2, true, "-"),
                listOf(Double.POSITIVE_INFINITY, 2, true, "-"),
                listOf(0.155, 2, false, "0.16"),
                listOf(0.0, 2, false, "0"),
                listOf(-1.155, 1, true, "-1.2"),
                listOf(-0.001, 2, true, "0.00"),
                listOf(1.0E-7, 8, true, "0.00000010"),
                listOf(1.5E20, 1, false, "150000000000000000000"),
                listOf(12345.678f, 1, true, "12345.7"),
            )
        }
    }

}