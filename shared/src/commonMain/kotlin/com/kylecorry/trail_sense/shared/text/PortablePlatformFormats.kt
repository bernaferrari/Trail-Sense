package com.kylecorry.trail_sense.shared.text

import com.kylecorry.andromeda.core.math.DecimalFormatter
import com.kylecorry.trail_sense.shared.time.DateNameWidth
import com.kylecorry.trail_sense.shared.time.displayName
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

class PortablePlatformFormats : PlatformFormats {

    override fun formatDate(
        date: Instant,
        includeWeekDay: Boolean,
        includeYear: Boolean,
        abbreviateMonth: Boolean
    ): String {
        val zone = TimeZone.currentSystemDefault()
        val local = date.toLocalDateTime(zone)
        val currentYear = Clock.System.now().toLocalDateTime(zone).year
        val showYear = includeYear || local.year != currentYear
        val month = local.month.displayName(
            if (abbreviateMonth) DateNameWidth.Short else DateNameWidth.Full
        )
        return buildString {
            if (includeWeekDay) {
                append(local.dayOfWeek.displayName(DateNameWidth.Full))
                append(", ")
            }
            append(month)
            append(' ')
            append(local.day)
            if (showYear) {
                append(", ")
                append(local.year)
            }
        }
    }

    override fun formatFileSize(bytes: Long, short: Boolean): String {
        val decimals = if (short) 0 else 1
        val abs = bytes.toDouble()
        return when {
            abs < 1024 -> "$bytes B"
            abs < 1024 * 1024 -> "${DecimalFormatter.format(abs / 1024.0, decimals, !short)} KB"
            abs < 1024 * 1024 * 1024 -> "${DecimalFormatter.format(abs / (1024.0 * 1024.0), decimals, !short)} MB"
            else -> "${DecimalFormatter.format(abs / (1024.0 * 1024.0 * 1024.0), decimals, !short)} GB"
        }
    }

    override val amPmMarkers: Pair<String, String> = "AM" to "PM"
}
