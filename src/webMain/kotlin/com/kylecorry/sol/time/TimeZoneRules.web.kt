package com.kylecorry.sol.time

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

// js-joda exposes offsets but not daylight savings rules, so derive them from the offsets
internal actual fun standardOffsetSeconds(zone: TimeZone, time: Instant): Int =
    estimateStandardOffsetSeconds(zone, time)

internal actual fun nextTimeZoneTransition(zone: TimeZone, time: Instant): Instant? =
    searchNextTimeZoneTransition(zone, time)
