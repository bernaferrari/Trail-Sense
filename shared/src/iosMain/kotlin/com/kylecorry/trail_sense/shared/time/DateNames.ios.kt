package com.kylecorry.trail_sense.shared.time

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSTimeZone
import platform.Foundation.NSTimeZoneNameStyle
import platform.Foundation.currentLocale
import platform.Foundation.localizedName
import platform.Foundation.timeZoneWithName

private fun formatter() = NSDateFormatter().apply { locale = NSLocale.currentLocale }

actual fun Month.displayName(width: DateNameWidth, standalone: Boolean): String {
    val f = formatter()
    val names = when (width) {
        DateNameWidth.Full -> if (standalone) f.standaloneMonthSymbols else f.monthSymbols
        DateNameWidth.Short -> if (standalone) f.shortStandaloneMonthSymbols else f.shortMonthSymbols
        DateNameWidth.Narrow -> if (standalone) f.veryShortStandaloneMonthSymbols else f.veryShortMonthSymbols
    }
    return names?.getOrNull(ordinal) as? String ?: name
}

actual fun DayOfWeek.displayName(width: DateNameWidth, standalone: Boolean): String {
    val f = formatter()
    val names = when (width) {
        DateNameWidth.Full -> if (standalone) f.standaloneWeekdaySymbols else f.weekdaySymbols
        DateNameWidth.Short -> if (standalone) f.shortStandaloneWeekdaySymbols else f.shortWeekdaySymbols
        DateNameWidth.Narrow -> if (standalone) f.veryShortStandaloneWeekdaySymbols else f.veryShortWeekdaySymbols
    }
    // Foundation lists weekdays from Sunday
    return names?.getOrNull(isoDayNumber % 7) as? String ?: name
}

actual fun TimeZone.displayName(): String {
    val zone = NSTimeZone.timeZoneWithName(id) ?: return id
    return zone.localizedName(NSTimeZoneNameStyle.NSTimeZoneNameStyleGeneric, NSLocale.currentLocale) ?: id
}
