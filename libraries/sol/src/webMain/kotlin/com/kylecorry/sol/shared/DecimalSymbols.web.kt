package com.kylecorry.sol.shared

internal actual fun platformDecimalSymbols(languageTag: String?): DecimalSymbols {
    val invariant = DecimalSymbols.Invariant
    return DecimalSymbols(
        decimalSeparator = intlNumberPart(languageTag, "decimal") ?: invariant.decimalSeparator,
        minusSign = intlNumberPart(languageTag, "minusSign") ?: invariant.minusSign,
        zeroDigit = intlNumberPart(languageTag, "integer")?.singleOrNull() ?: invariant.zeroDigit
    )
}

/**
 * Formats -0.5 with Intl.NumberFormat and returns the part of the given [type], or null if the locale is unknown.
 * A null [languageTag] uses the runtime's default locale.
 */
@Suppress("UnusedParameter") // Read by the JavaScript code
private fun intlNumberPart(languageTag: String?, type: String): String? = js(
    """(function () {
        try {
            var parts = new Intl.NumberFormat(languageTag == null ? undefined : languageTag).formatToParts(-0.5);
            for (var i = 0; i < parts.length; i++) {
                if (parts[i].type === type) return parts[i].value;
            }
        } catch (e) {}
        return null;
    })()"""
)
