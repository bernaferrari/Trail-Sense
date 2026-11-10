package com.kylecorry.andromeda.background

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Decision for a daily task: execute at most once per local date, then schedule the next occurrence. */
public data class DailyScheduleDecision(val shouldRun: Boolean, val localDate: LocalDate, val nextRun: Instant)

/**
 * Plans one delivery using the host's persisted [lastRun] date. Record [DailyScheduleDecision.localDate]
 * before running when duplicate execution after a crash would be undesirable.
 */
public fun planDailyTask(
    now: Instant,
    time: LocalTime,
    timeZone: TimeZone,
    lastRun: LocalDate? = null,
    enabled: Boolean = true,
    tolerance: Duration = 30.minutes,
): DailyScheduleDecision {
    require(tolerance.isFinite() && tolerance >= Duration.ZERO)
    val today = now.toLocalDateTime(timeZone).date
    val scheduled = LocalDateTime(today, time).toInstant(timeZone)
    val early = now < scheduled - tolerance
    val due = !early && now <= scheduled + tolerance && enabled && lastRun != today
    val next = if (early) scheduled else LocalDateTime(today.plus(DatePeriod(days = 1)), time).toInstant(timeZone)
    return DailyScheduleDecision(due, today, next)
}
