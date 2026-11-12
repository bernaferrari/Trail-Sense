package com.kylecorry.trail_sense.shared.time

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone

enum class DateNameWidth {
    Full,
    Short,
    Narrow
}

/**
 * The month's name in the user's language. Standalone names are the nominative form used in labels and
 * headings, the default form is the one used inside a formatted date.
 */
expect fun Month.displayName(width: DateNameWidth = DateNameWidth.Full, standalone: Boolean = false): String

/** The day of the week's name in the user's language. */
expect fun DayOfWeek.displayName(width: DateNameWidth = DateNameWidth.Full, standalone: Boolean = false): String

/** The time zone's generic name in the user's language (e.g. "Brasilia Time"), falling back to its ID. */
expect fun TimeZone.displayName(): String
