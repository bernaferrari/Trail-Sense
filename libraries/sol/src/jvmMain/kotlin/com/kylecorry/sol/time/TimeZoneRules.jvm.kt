package com.kylecorry.sol.time

import java.time.ZoneId
import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

internal actual fun standardOffsetSeconds(zone: TimeZone, time: Instant): Int =
    ZoneId.of(zone.id).rules.getStandardOffset(time.toJavaInstant()).totalSeconds

internal actual fun nextTimeZoneTransition(zone: TimeZone, time: Instant): Instant? =
    ZoneId.of(zone.id).rules.nextTransition(time.toJavaInstant())?.instant?.toKotlinInstant()
