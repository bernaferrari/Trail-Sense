package com.kylecorry.trail_sense.shared.time

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone

private val fullMonths = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)
private val shortMonths = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)
private val narrowMonths = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")

private val fullWeekdays = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
)
private val shortWeekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val narrowWeekdays = listOf("M", "T", "W", "T", "F", "S", "S")

actual fun Month.displayName(width: DateNameWidth, standalone: Boolean): String = when (width) {
    DateNameWidth.Full -> fullMonths.getOrElse(ordinal) { name }
    DateNameWidth.Short -> shortMonths.getOrElse(ordinal) { name }
    DateNameWidth.Narrow -> narrowMonths.getOrElse(ordinal) { name }
}

actual fun DayOfWeek.displayName(width: DateNameWidth, standalone: Boolean): String = when (width) {
    DateNameWidth.Full -> fullWeekdays.getOrElse(ordinal) { name }
    DateNameWidth.Short -> shortWeekdays.getOrElse(ordinal) { name }
    DateNameWidth.Narrow -> narrowWeekdays.getOrElse(ordinal) { name }
}

actual fun TimeZone.displayName(): String = id
