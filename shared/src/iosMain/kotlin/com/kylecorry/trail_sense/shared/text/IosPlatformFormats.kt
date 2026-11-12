package com.kylecorry.trail_sense.shared.text

import platform.Foundation.NSByteCountFormatter
import platform.Foundation.NSByteCountFormatterCountStyleFile
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlin.time.Instant

class IosPlatformFormats : PlatformFormats {

    override fun formatDate(
        date: Instant,
        includeWeekDay: Boolean,
        includeYear: Boolean,
        abbreviateMonth: Boolean
    ): String {
        val nsDate = NSDate.dateWithTimeIntervalSince1970(date.toEpochMilliseconds() / 1000.0)
        val calendar = NSCalendar.currentCalendar
        val showYear = includeYear ||
                calendar.component(NSCalendarUnitYear, nsDate) != calendar.component(NSCalendarUnitYear, NSDate())
        // The locale decides the order and punctuation; the template only names the fields
        val template = buildString {
            if (includeWeekDay) append("EEEE")
            append(if (abbreviateMonth) "MMM" else "MMMM")
            append("d")
            if (showYear) append("y")
        }
        val formatter = NSDateFormatter().apply {
            locale = NSLocale.currentLocale
            setLocalizedDateFormatFromTemplate(template)
        }
        return formatter.stringFromDate(nsDate)
    }

    override fun formatFileSize(bytes: Long, short: Boolean): String {
        val formatter = NSByteCountFormatter().apply {
            countStyle = NSByteCountFormatterCountStyleFile
            zeroPadsFractionDigits = !short
        }
        return formatter.stringFromByteCount(bytes)
    }

    override val amPmMarkers: Pair<String, String>
        get() {
            val formatter = NSDateFormatter().apply { locale = NSLocale.currentLocale }
            return formatter.AMSymbol to formatter.PMSymbol
        }
}
