package com.kylecorry.sol.science.oceanography

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.datetime.TimeZone
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

internal fun Tide(time: ZonedDateTime, isHigh: Boolean, height: Float? = null): Tide =
    Tide(time.toInstant().toKotlinInstant(), isHigh, height, TimeZone.of(time.zone.id))

internal fun Tide.Companion.high(time: ZonedDateTime, height: Float? = null): Tide = Tide(time, true, height)
internal fun Tide.Companion.low(time: ZonedDateTime, height: Float? = null): Tide = Tide(time, false, height)
internal fun Tide.toJavaZonedDateTime(): ZonedDateTime = time.toJavaInstant().atZone(ZoneId.of(timeZone.id))
