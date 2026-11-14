package com.kylecorry.sol.tests

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/** An instant given as a universal (UTC) calendar date and time. */
fun ut(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0): Instant =
    LocalDateTime(year, month, day, hour, minute, second).toInstant(TimeZone.UTC)
