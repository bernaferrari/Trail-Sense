package com.kylecorry.sol.time

import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toKotlinInstant
import kotlinx.datetime.toNSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.timeZoneWithName
import kotlin.time.Instant

internal actual fun standardOffsetSeconds(zone: TimeZone, time: Instant): Int {
    val nsZone = NSTimeZone.timeZoneWithName(zone.id) ?: return zone.offsetAt(time).totalSeconds
    val date = time.toNSDate()
    return (nsZone.secondsFromGMTForDate(date) - nsZone.daylightSavingTimeOffsetForDate(date)).toInt()
}

internal actual fun nextTimeZoneTransition(zone: TimeZone, time: Instant): Instant? {
    val nsZone = NSTimeZone.timeZoneWithName(zone.id)
    return nsZone?.nextDaylightSavingTimeTransitionAfterDate(time.toNSDate())?.toKotlinInstant()
}
