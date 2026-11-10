package com.kylecorry.sol.science.oceanography.waterlevel

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

public interface IWaterLevelCalculator {
    public fun calculate(time: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): Float
}
