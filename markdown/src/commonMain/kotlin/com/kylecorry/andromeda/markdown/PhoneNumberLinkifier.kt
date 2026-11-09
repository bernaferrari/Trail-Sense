package com.kylecorry.andromeda.markdown

/**
 * Finds phone numbers in plain text, like Android's Linkify, without a phone number database: international
 * (`+44 20 7946 0018`), parenthesized area codes (`(555) 123-4567`) and digit groups split by spaces, dots or
 * hyphens (`555-123-4567`, `555.123.4567`), with 7 to 15 digits. Dates, decimals, coordinates and IP addresses are
 * not phone numbers.
 */
public object PhoneNumberLinkifier {

    /** A phone number at [range] in the text, dialed with [url] (`tel:` and its digits). */
    public data class Match(val range: IntRange, val url: String)

    public fun find(text: CharSequence): List<Match> {
        return CANDIDATE.findAll(text).mapNotNull { candidate ->
            val value = candidate.value
            if (!isPhoneNumber(value)) return@mapNotNull null
            val digits = value.filter { it.isAsciiDigit() }
            val prefix = if (value.startsWith('+')) "+" else ""
            Match(candidate.range, "tel:$prefix$digits")
        }.toList()
    }

    private fun isPhoneNumber(value: String): Boolean {
        val digitCount = value.count { it.isAsciiDigit() }
        if (digitCount !in MIN_DIGITS..MAX_DIGITS) return false
        if (DATE.matches(value)) return false
        val international = value.startsWith('+') || value.contains('(')
        val groups = value.split(SEPARATORS).filter { it.isNotEmpty() }
        val separators = value.filter { it == '.' || it == '-' || it == ' ' || it == '\u00A0' }.toSet()
        if (separators == setOf('.')) {
            // Dotted numbers read as decimals or IP addresses unless every group has at least two digits
            if (groups.size < 3 || groups.any { it.length < 2 }) return false
        }
        if (!international && groups.size > 1) {
            if (groups.last().length < 2) return false
            if (groups.count { it.length == 1 } > 1) return false
        }
        return true
    }

    private fun Char.isAsciiDigit() = this in '0'..'9'

    private const val MIN_DIGITS = 7
    private const val MAX_DIGITS = 15
    private val SEPARATORS = Regex("[ \\u00A0.()+-]")
    private val DATE = Regex("""\d{4}[-./ ]\d{1,2}[-./ ]\d{1,2}|\d{1,2}[-./ ]\d{1,2}[-./ ]\d{2,4}""")
    private val CANDIDATE = Regex(
        """(?<![\p{L}\p{N}_+\-./#:@])""" +
            """(?:\+\d{1,3}[ \u00A0.-]?)?""" +
            """(?:\(\d{1,4}\)[ \u00A0.-]?)?""" +
            """\d{1,4}(?:[ \u00A0.-]?\d{1,4}){1,5}""" +
            """(?![\p{L}\p{N}_@/]|[.\-][\p{L}\p{N}])"""
    )
}
