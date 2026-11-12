package com.kylecorry.trail_sense.shared.text

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import java.text.DateFormatSymbols
import java.util.Locale
import kotlin.time.Instant

class AndroidPlatformFormats(context: Context) : PlatformFormats {

    private val context = context.applicationContext

    override fun formatDate(
        date: Instant,
        includeWeekDay: Boolean,
        includeYear: Boolean,
        abbreviateMonth: Boolean
    ): String {
        var flags = DateUtils.FORMAT_SHOW_DATE
        if (includeWeekDay) flags = flags or DateUtils.FORMAT_SHOW_WEEKDAY
        flags = flags or if (includeYear) DateUtils.FORMAT_SHOW_YEAR else DateUtils.FORMAT_ABBREV_RELATIVE
        if (abbreviateMonth) flags = flags or DateUtils.FORMAT_ABBREV_MONTH
        return DateUtils.formatDateTime(context, date.toEpochMilliseconds(), flags)
    }

    override fun formatFileSize(bytes: Long, short: Boolean): String {
        return if (short) {
            Formatter.formatShortFileSize(context, bytes)
        } else {
            Formatter.formatFileSize(context, bytes)
        }
    }

    override val amPmMarkers: Pair<String, String>
        get() {
            val markers = DateFormatSymbols.getInstance(Locale.getDefault()).amPmStrings
            return markers[0] to markers[1]
        }
}
