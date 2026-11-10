package com.kylecorry.sol.shared

/**
 * The symbols a locale uses to write a decimal number.
 */
internal data class DecimalSymbols(
    val decimalSeparator: String,
    val minusSign: String,
    val zeroDigit: Char
) {
    companion object {
        val Invariant = DecimalSymbols(decimalSeparator = ".", minusSign = "-", zeroDigit = '0')

        fun of(locale: NumberLocale): DecimalSymbols = when (locale) {
            NumberLocale.Invariant -> Invariant
            NumberLocale.System -> platformDecimalSymbols(languageTag = null)
            is NumberLocale.Tag -> platformDecimalSymbols(locale.languageTag)
        }
    }
}

/**
 * Reads the decimal symbols of the locale with the given BCP 47 [languageTag], or of the user's current
 * formatting locale when it is null.
 */
internal expect fun platformDecimalSymbols(languageTag: String?): DecimalSymbols
