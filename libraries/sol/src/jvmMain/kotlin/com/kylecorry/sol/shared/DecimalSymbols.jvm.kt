package com.kylecorry.sol.shared

import java.text.DecimalFormatSymbols
import java.util.Locale

internal actual fun platformDecimalSymbols(languageTag: String?): DecimalSymbols {
    val locale = languageTag?.let(Locale::forLanguageTag) ?: Locale.getDefault(Locale.Category.FORMAT)
    val symbols = DecimalFormatSymbols.getInstance(locale)
    return DecimalSymbols(
        decimalSeparator = symbols.decimalSeparator.toString(),
        minusSign = symbols.minusSign.toString(),
        zeroDigit = symbols.zeroDigit
    )
}
