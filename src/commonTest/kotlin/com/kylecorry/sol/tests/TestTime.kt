package com.kylecorry.sol.tests

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * An instant paired with the time zone it was written in. Tests use it where a wall-clock time in
 * a specific zone matters (sunrise on a given local date, tides in local time, etc.).
 */
data class ZonedTime(val instant: Instant, val zone: TimeZone) : Comparable<ZonedTime> {
    val local: LocalDateTime get() = instant.toLocalDateTime(zone)
    val date: LocalDate get() = local.date
    val time: LocalTime get() = local.time
    val year: Int get() = local.year

    operator fun plus(duration: Duration): ZonedTime = copy(instant = instant + duration)
    operator fun minus(duration: Duration): ZonedTime = copy(instant = instant - duration)
    operator fun minus(other: ZonedTime): Duration = instant - other.instant

    /** Adds calendar days, keeping the local time of day (like ZonedDateTime.plusDays). */
    fun plusDays(days: Int): ZonedTime = of(date.plus(days, DateTimeUnit.DAY).atTime(time), zone)

    fun withZone(zone: TimeZone): ZonedTime = copy(zone = zone)

    override fun compareTo(other: ZonedTime): Int = instant.compareTo(other.instant)

    override fun toString(): String = "$local[${zone.id}]"

    companion object {
        fun of(local: LocalDateTime, zone: TimeZone): ZonedTime = ZonedTime(local.toInstant(zone), zone)
    }
}

fun Instant.atZone(zone: TimeZone): ZonedTime = ZonedTime(this, zone)

fun LocalDateTime.atZone(zone: TimeZone): ZonedTime = ZonedTime.of(this, zone)

fun zoned(
    year: Int,
    month: Int,
    day: Int,
    hour: Int = 0,
    minute: Int = 0,
    second: Int = 0,
    nanosecond: Int = 0,
    zone: TimeZone = TimeZone.UTC
): ZonedTime = ZonedTime.of(LocalDateTime(year, month, day, hour, minute, second, nanosecond), zone)

fun zoned(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int, zone: String): ZonedTime =
    zoned(year, month, day, hour, minute, second, 0, TimeZone.of(zone))

fun zone(id: String): TimeZone = TimeZone.of(id)

private val zonedPattern = Regex(
    """^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2}(?:\.\d+)?)?)(Z|[+-]\d{2}(?::?\d{2})?)?(?:\[([^\]]+)])?$"""
)

/**
 * Parses ISO-8601 date times in the formats java.time.ZonedDateTime.parse accepted in the old JVM
 * tests: `2020-09-12T12:00:00-04`, `2026-04-10T09:00:00-04:00[America/New_York]`, `...Z`.
 * Without a region the zone is the fixed offset; without an offset the region decides it.
 */
fun zoned(text: String): ZonedTime {
    val match = zonedPattern.matchEntire(text.trim()) ?: error("Unsupported zoned date time: $text")
    val local = LocalDateTime.parse(match.groupValues[1])
    val offsetText = match.groupValues[2]
    val region = match.groupValues[3]
    val offset = if (offsetText.isEmpty()) null else parseOffset(offsetText)
    val zone = when {
        region.isNotEmpty() -> TimeZone.of(region)
        offset != null -> FixedOffsetTimeZone(offset)
        else -> error("Zoned date time needs an offset or a region: $text")
    }
    val instant = if (offset != null) local.toInstant(offset) else local.toInstant(zone)
    return ZonedTime(instant, zone)
}

private fun parseOffset(text: String): UtcOffset {
    if (text == "Z") return UtcOffset.ZERO
    val sign = if (text[0] == '-') -1 else 1
    val digits = text.substring(1).replace(":", "")
    val hours = digits.substring(0, 2).toInt()
    val minutes = if (digits.length > 2) digits.substring(2, 4).toInt() else 0
    return UtcOffset(hours = sign * hours, minutes = sign * minutes)
}
