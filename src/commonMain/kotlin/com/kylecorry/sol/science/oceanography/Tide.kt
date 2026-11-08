package com.kylecorry.sol.science.oceanography

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

data class Tide(
    val time: Instant,
    val isHigh: Boolean,
    val height: Float? = null,
    val timeZone: TimeZone = TimeZone.currentSystemDefault()
) {
    companion object {
        fun high(time: Instant, height: Float? = null, timeZone: TimeZone = TimeZone.currentSystemDefault()): Tide =
            Tide(time, true, height, timeZone)

        fun low(time: Instant, height: Float? = null, timeZone: TimeZone = TimeZone.currentSystemDefault()): Tide =
            Tide(time, false, height, timeZone)
    }
}
