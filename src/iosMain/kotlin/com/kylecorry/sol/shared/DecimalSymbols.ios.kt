package com.kylecorry.sol.shared

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.canonicalLocaleIdentifierFromString
import platform.Foundation.currentLocale

internal actual fun platformDecimalSymbols(languageTag: String?): DecimalSymbols {
    val formatter = NSNumberFormatter()
    formatter.locale = if (languageTag == null) {
        NSLocale.currentLocale
    } else {
        NSLocale(localeIdentifier = NSLocale.canonicalLocaleIdentifierFromString(languageTag))
    }
    return DecimalSymbols(
        decimalSeparator = formatter.decimalSeparator,
        minusSign = formatter.minusSign,
        zeroDigit = formatter.stringFromNumber(NSNumber(int = 0))?.singleOrNull() ?: '0'
    )
}
