package com.kylecorry.sol.science.oceanography

import com.kylecorry.sol.tests.ZonedTime

internal fun Tide(time: ZonedTime, isHigh: Boolean, height: Float? = null): Tide =
    Tide(time.instant, isHigh, height, time.zone)

internal fun Tide.Companion.high(time: ZonedTime, height: Float? = null): Tide = Tide(time, true, height)

internal fun Tide.Companion.low(time: ZonedTime, height: Float? = null): Tide = Tide(time, false, height)

internal val Tide.zonedTime: ZonedTime
    get() = ZonedTime(time, timeZone)
