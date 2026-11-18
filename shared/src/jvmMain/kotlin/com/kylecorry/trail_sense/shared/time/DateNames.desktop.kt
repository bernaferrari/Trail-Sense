package com.kylecorry.trail_sense.shared.time

import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone

actual fun Month.displayName(width: DateNameWidth, standalone: Boolean): String =
    java.time.Month.valueOf(name).getDisplayName(width.toJavaStyle(standalone), Locale.getDefault())

actual fun DayOfWeek.displayName(width: DateNameWidth, standalone: Boolean): String =
    java.time.DayOfWeek.valueOf(name).getDisplayName(width.toJavaStyle(standalone), Locale.getDefault())

actual fun TimeZone.displayName(): String =
    java.time.ZoneId.of(id).getDisplayName(TextStyle.FULL, Locale.getDefault())

private fun DateNameWidth.toJavaStyle(standalone: Boolean): TextStyle {
    val style = when (this) {
        DateNameWidth.Full -> TextStyle.FULL
        DateNameWidth.Short -> TextStyle.SHORT
        DateNameWidth.Narrow -> TextStyle.NARROW
    }
    return if (standalone) style.asStandalone() else style
}
