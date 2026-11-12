package com.kylecorry.trail_sense.shared.time

import android.icu.text.DateFormatSymbols
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import java.util.Locale

actual fun Month.displayName(width: DateNameWidth, standalone: Boolean): String {
    return displayName(width, standalone, Locale.getDefault())
}

/** The month's name in [locale] (such as the composition's), see the common displayName. */
fun Month.displayName(
    width: DateNameWidth = DateNameWidth.Full,
    standalone: Boolean = false,
    locale: Locale
): String {
    val symbols = DateFormatSymbols.getInstance(locale)
    return symbols.getMonths(context(standalone), width.icu())[ordinal]
}

actual fun DayOfWeek.displayName(width: DateNameWidth, standalone: Boolean): String {
    val symbols = DateFormatSymbols.getInstance(Locale.getDefault())
    // ICU weekdays are indexed by Calendar day (Sunday = 1)
    return symbols.getWeekdays(context(standalone), width.icu())[isoDayNumber % 7 + 1]
}

actual fun TimeZone.displayName(): String {
    val zone = android.icu.util.TimeZone.getTimeZone(id)
    return zone.getDisplayName(false, android.icu.util.TimeZone.LONG_GENERIC, Locale.getDefault()) ?: id
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
