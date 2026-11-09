package com.kylecorry.andromeda.core.time

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** A point in time together with the time zone used to display it. */
data class ZonedDateTime(val instant: Instant, val timeZone: TimeZone) {
    val localDateTime: LocalDateTime get() = instant.toLocalDateTime(timeZone)
}

fun interface ZonedDateTimeProvider {
    fun getTime(): ZonedDateTime
}
