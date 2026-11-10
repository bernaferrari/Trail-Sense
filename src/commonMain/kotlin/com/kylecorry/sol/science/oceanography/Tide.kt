package com.kylecorry.sol.science.oceanography

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

public data class Tide(
    val time: Instant,
    val isHigh: Boolean,
    val height: Float? = null,
    val timeZone: TimeZone = TimeZone.currentSystemDefault()
) {
    public companion object {
        public fun high(
            time: Instant,
            height: Float? = null,
            timeZone: TimeZone = TimeZone.currentSystemDefault()
        ): Tide =
            Tide(time, true, height, timeZone)

        public fun low(
            time: Instant,
            height: Float? = null,
            timeZone: TimeZone = TimeZone.currentSystemDefault()
        ): Tide =
            Tide(time, false, height, timeZone)
    }
}
