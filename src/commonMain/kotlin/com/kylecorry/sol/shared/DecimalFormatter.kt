package com.kylecorry.sol.shared

import com.kylecorry.sol.math.MathExtensions.roundPlaces
import kotlin.math.abs

/**
 * Formats numbers without grouping, using the decimal separator, minus sign, and digits of a [NumberLocale].
 */
internal object DecimalFormatter {

    /**
     * @param decimalPlaces the maximum number of fraction digits (rounded half up)
     * @param strict when true, always shows [decimalPlaces] fraction digits; otherwise trailing
     * zeros are dropped. Zero is never shown with a minus sign.
     * @param locale the locale whose number symbols are used, the user's locale by default
     * @return the formatted number, or "-" when it is not finite
     */
    fun format(
        number: Number,
        decimalPlaces: Int,
        strict: Boolean = true,
        locale: NumberLocale = NumberLocale.System
    ): String {
        val value = number.toDouble()
        if (!value.isFinite()) {
            return "-"
        }
        val invariant = formatInvariant(value, decimalPlaces, strict)
        return if (locale == NumberLocale.Invariant) invariant else localize(invariant, DecimalSymbols.of(locale))
    }

    private fun localize(invariant: String, symbols: DecimalSymbols): String {
        if (symbols == DecimalSymbols.Invariant) {
            return invariant
        }
        return buildString(invariant.length + 2) {
            for (char in invariant) {
                when (char) {
                    '-' -> append(symbols.minusSign)
                    '.' -> append(symbols.decimalSeparator)
                    else -> append(symbols.zeroDigit + (char - '0'))
                }
            }
        }
    }

    private fun formatInvariant(value: Double, decimalPlaces: Int, strict: Boolean): String {
        val places = decimalPlaces.coerceAtLeast(0)
        val rounded = value.roundPlaces(places)
        val (integerDigits, fractionDigits) = plainDigits(abs(rounded))
        val fraction = fractionDigits.take(places).let {
            if (strict) it.padEnd(places, '0') else it.trimEnd('0')
        }
        val isZero = integerDigits == "0" && fraction.all { it == '0' }
        val sign = if (rounded < 0 && !isZero) "-" else ""
        return if (fraction.isEmpty()) "$sign$integerDigits" else "$sign$integerDigits.$fraction"
    }

    /**
     * Splits a non-negative finite double into its integer and fraction digits without
     * scientific notation, using the shortest representation that round-trips.
     */
    private fun plainDigits(value: Double): Pair<String, String> {
        val text = value.toString()
        val exponentIndex = text.indexOfFirst { it == 'E' || it == 'e' }
        val mantissa = if (exponentIndex >= 0) text.substring(0, exponentIndex) else text
        val exponent = if (exponentIndex >= 0) text.substring(exponentIndex + 1).toInt() else 0
        val pointIndex = mantissa.indexOf('.')
        val integerPart = if (pointIndex >= 0) mantissa.substring(0, pointIndex) else mantissa
        val fractionPart = if (pointIndex >= 0) mantissa.substring(pointIndex + 1) else ""
        val digits = integerPart + fractionPart
        val pointPosition = integerPart.length + exponent
        val integerDigits = when {
            pointPosition <= 0 -> "0"
            pointPosition >= digits.length -> digits.padEnd(pointPosition, '0')
            else -> digits.substring(0, pointPosition)
        }.trimStart('0').ifEmpty { "0" }
        val fractionDigits = when {
            pointPosition <= 0 -> "0".repeat(-pointPosition) + digits
            pointPosition >= digits.length -> ""
            else -> digits.substring(pointPosition)
        }
        return integerDigits to fractionDigits
    }
}
