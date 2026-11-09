package com.kylecorry.sol.shared

/**
 * The locale whose number symbols (decimal separator, minus sign, and digits) are used when formatting numbers.
 */
sealed interface NumberLocale {

    /**
     * The user's current formatting locale: `Locale.getDefault(Locale.Category.FORMAT)` on the JVM and
     * Android, `NSLocale.currentLocale` on iOS. Use this for text shown to the user.
     */
    data object System : NumberLocale

    /**
     * A '.' decimal separator, '-' minus sign, and ASCII digits in every locale. Use this for text that is
     * stored, exchanged, or parsed by other programs.
     */
    data object Invariant : NumberLocale

    /**
     * A specific locale, identified by a BCP 47 language tag such as "pt-BR".
     */
    data class Tag(val languageTag: String) : NumberLocale
}
