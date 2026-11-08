package com.kylecorry.sol.time

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

internal expect fun standardOffsetSeconds(zone: TimeZone, time: Instant): Int
internal expect fun nextTimeZoneTransition(zone: TimeZone, time: Instant): Instant?
