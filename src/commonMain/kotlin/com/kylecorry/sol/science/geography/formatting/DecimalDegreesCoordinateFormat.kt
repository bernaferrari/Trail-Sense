package com.kylecorry.sol.science.geography.formatting

import com.kylecorry.sol.shared.DecimalFormatter
import com.kylecorry.sol.shared.NumberLocale
import com.kylecorry.sol.shared.toDoubleCompat
import com.kylecorry.sol.units.Coordinate

/**
 * Decimal degrees, such as "41.123456°,  -71.123456°".
 * @param precision the number of decimal places
 * @param locale the locale whose decimal separator, minus sign, and digits are used. Text in any locale
 * (or [NumberLocale.Invariant] for machine-readable text) can be parsed back.
 */
class DecimalDegreesCoordinateFormat(
    private val precision: Int = 6,
    private val locale: NumberLocale = NumberLocale.System
) : CoordinateFormat {
    override fun toString(coordinate: Coordinate): String {
        val formattedLatitude = DecimalFormatter.format(coordinate.latitude, precision, locale = locale)
        val formattedLongitude = DecimalFormatter.format(coordinate.longitude, precision, locale = locale)
        return "$formattedLatitude°,  $formattedLongitude°"
    }

    override fun parse(text: String): Coordinate? {
        val regex = Regex("^(-?\\d+(?:[.,]\\d+)?)°?[,\\s]+(-?\\d+(?:[.,]\\d+)?)°?$")
        val matches = regex.find(normalizeNumberSymbols(text).trim()) ?: return null
        val latitude = matches.groupValues[1].toDoubleCompat() ?: return null
        val longitude = matches.groupValues[2].toDoubleCompat() ?: return null

        if (Coordinate.isValidLatitude(latitude) && Coordinate.isValidLongitude(longitude)) {
            return Coordinate(latitude, longitude)
        }

        return null
    }
}

/**
 * Rewrites localized digits, minus signs, and the Arabic decimal separator as ASCII so numbers
 * formatted in any locale can be parsed.
 */
private fun normalizeNumberSymbols(text: String): String = buildString(text.length) {
    for (char in text) {
        when {
            char == '\u2212' -> append('-')
            char == '\u066B' -> append('.')
            char == '\u200E' || char == '\u200F' || char == '\u061C' -> Unit
            char.isDigit() -> append('0' + char.digitToInt())
            else -> append(char)
        }
    }
}
