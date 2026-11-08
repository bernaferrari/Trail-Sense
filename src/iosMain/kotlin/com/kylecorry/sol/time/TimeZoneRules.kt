package com.kylecorry.sol.time

import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import platform.Foundation.*
import kotlin.time.Instant

private fun Instant.date() = NSDate(timeIntervalSinceReferenceDate = epochSeconds.toDouble() + nanosecondsOfSecond / 1e9 - 978307200.0)

internal actual fun standardOffsetSeconds(zone: TimeZone, time: Instant): Int {
    val nsZone = NSTimeZone.timeZoneWithName(zone.id) ?: return zone.offsetAt(time).totalSeconds
    val date = time.date()
    return (nsZone.secondsFromGMTForDate(date) - nsZone.daylightSavingTimeOffsetForDate(date)).toInt()
}

internal actual fun nextTimeZoneTransition(zone: TimeZone, time: Instant): Instant? {
    val nsZone = NSTimeZone.timeZoneWithName(zone.id) ?: return null
    val transition = nsZone.nextDaylightSavingTimeTransitionAfterDate(time.date()) ?: return null
    return Instant.fromEpochMilliseconds((transition.timeIntervalSince1970 * 1000).toLong())
}
