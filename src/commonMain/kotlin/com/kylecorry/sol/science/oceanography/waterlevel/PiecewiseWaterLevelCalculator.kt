package com.kylecorry.sol.science.oceanography.waterlevel

import com.kylecorry.sol.math.Range
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

class PiecewiseWaterLevelCalculator(private val calculators: List<Pair<Range<Instant>, IWaterLevelCalculator>>) :
    IWaterLevelCalculator {
    override fun calculate(time: Instant, timeZone: TimeZone): Float {
        return calculators.firstOrNull { time in it.first }?.second?.calculate(time, timeZone) ?: 0f
    }
}
