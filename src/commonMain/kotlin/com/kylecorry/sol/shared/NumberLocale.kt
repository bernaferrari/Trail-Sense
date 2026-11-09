package com.kylecorry.sol.shared

/**
 * The locale whose number symbols (decimal separator, minus sign, and digits) are used when formatting numbers.
 */
public sealed interface NumberLocale {

    /**
     * The user's current formatting locale: `Locale.getDefault(Locale.Category.FORMAT)` on the JVM and
     * Android, `NSLocale.currentLocale` on iOS. Use this for text shown to the user.
     */
    public data object System : NumberLocale

    /**
     * A '.' decimal separator, '-' minus sign, and ASCII digits in every locale. Use this for text that is
     * stored, exchanged, or parsed by other programs.
     */
    public data object Invariant : NumberLocale

    /**
     * A specific locale, identified by a BCP 47 language tag such as "pt-BR".
     */
    public data class Tag(val languageTag: String) : NumberLocale
}
