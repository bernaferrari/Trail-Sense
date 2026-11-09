package com.kylecorry.trail_sense.shared.time

import android.icu.text.DateFormatSymbols
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.isoDayNumber
import java.util.Locale

enum class DateNameWidth {
    Full,
    Short,
    Narrow
}

/**
 * The localized name of the month. Standalone names are the nominative form used in labels and headings,
 * the default form is the one used inside a formatted date.
 */
fun Month.displayName(
    width: DateNameWidth = DateNameWidth.Full,
    standalone: Boolean = false,
    locale: Locale = Locale.getDefault()
): String {
    val symbols = DateFormatSymbols.getInstance(locale)
    return symbols.getMonths(context(standalone), width.icu())[ordinal]
}

/**
 * The localized name of the day of the week.
 */
fun DayOfWeek.displayName(
    width: DateNameWidth = DateNameWidth.Full,
    standalone: Boolean = false,
    locale: Locale = Locale.getDefault()
): String {
    val symbols = DateFormatSymbols.getInstance(locale)
    // ICU weekdays are indexed by Calendar day (Sunday = 1)
    return symbols.getWeekdays(context(standalone), width.icu())[isoDayNumber % 7 + 1]
}

private fun context(standalone: Boolean): Int {
    return if (standalone) DateFormatSymbols.STANDALONE else DateFormatSymbols.FORMAT
}

private fun DateNameWidth.icu(): Int {
    return when (this) {
        DateNameWidth.Full -> DateFormatSymbols.WIDE
        DateNameWidth.Short -> DateFormatSymbols.ABBREVIATED
        DateNameWidth.Narrow -> DateFormatSymbols.NARROW
    }
}

/**
 * The localized generic name of the time zone (e.g. "Brasilia Time"), falling back to its ID.
 */
fun kotlinx.datetime.TimeZone.displayName(locale: Locale = Locale.getDefault()): String {
    val zone = android.icu.util.TimeZone.getTimeZone(id)
    return zone.getDisplayName(false, android.icu.util.TimeZone.LONG_GENERIC, locale) ?: id
}
