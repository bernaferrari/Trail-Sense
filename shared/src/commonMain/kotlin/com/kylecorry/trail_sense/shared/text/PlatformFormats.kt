package com.kylecorry.trail_sense.shared.text

import kotlin.time.Instant

/** Locale-aware formats each platform already knows how to write in the user's language. */
interface PlatformFormats {

    /**
     * A calendar date in the locale's style, such as "Tuesday, March 4, 2025", in the system time zone.
     * @param includeYear false shows the year only when it isn't the current one
     */
    fun formatDate(
        date: Instant,
        includeWeekDay: Boolean,
        includeYear: Boolean,
        abbreviateMonth: Boolean
    ): String

    /** A file size in the locale's units and digits, such as "1.2 MB". */
    fun formatFileSize(bytes: Long, short: Boolean): String

    /** The locale's markers for the morning and afternoon on a 12 hour clock. */
    val amPmMarkers: Pair<String, String>
}
