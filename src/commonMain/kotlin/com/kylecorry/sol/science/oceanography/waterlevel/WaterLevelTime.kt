package com.kylecorry.sol.science.oceanography.waterlevel

import kotlin.time.Instant

internal fun hoursBetween(first: Instant, second: Instant): Float {
    val seconds = second.epochSeconds - first.epochSeconds -
        if (second.nanosecondsOfSecond < first.nanosecondsOfSecond) 1 else 0
    return seconds / 3600f
}
