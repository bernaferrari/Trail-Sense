package com.kylecorry.sol.science.oceanography.waterlevel

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

interface IWaterLevelCalculator {
    fun calculate(time: Instant, timeZone: TimeZone = TimeZone.currentSystemDefault()): Float
}
