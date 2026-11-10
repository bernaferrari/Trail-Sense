package com.kylecorry.sol.science.geography

import com.kylecorry.sol.science.geography.formatting.DecimalDegreesCoordinateFormat
import com.kylecorry.sol.shared.NumberLocale
import com.kylecorry.sol.units.Coordinate
import kotlin.test.Test
import kotlin.test.assertEquals

class DecimalDegreesLocaleTest {

    private val coordinate = Coordinate(41.5, -71.25)

    @Test
    fun formatsWithTheLocaleDecimalSeparator() {
        val format = DecimalDegreesCoordinateFormat(4, NumberLocale.Tag("pt-BR"))
        assertEquals("41,5000°,  -71,2500°", format.toString(coordinate))
    }

    @Test
    fun invariantFormatUsesAPeriod() {
        val format = DecimalDegreesCoordinateFormat(4, NumberLocale.Invariant)
        assertEquals("41.5000°,  -71.2500°", format.toString(coordinate))
    }

    @Test
    fun localizedTextParsesBack() {
        for (locale in listOf(NumberLocale.Tag("pt-BR"), NumberLocale.Tag("en-US"), NumberLocale.Invariant)) {
            val format = DecimalDegreesCoordinateFormat(4, locale)
            assertEquals(coordinate, format.parse(format.toString(coordinate)), locale.toString())
        }
    }

    @Test
    fun parsesUnicodeMinusSignsAndDigits() {
        val format = DecimalDegreesCoordinateFormat()
        assertEquals(coordinate, format.parse("41.5°, \u221271.25°"))
        assertEquals(coordinate, format.parse("\u0664\u0661\u066B\u0665°, -\u0667\u0661\u066B\u0662\u0665°"))
    }
}
