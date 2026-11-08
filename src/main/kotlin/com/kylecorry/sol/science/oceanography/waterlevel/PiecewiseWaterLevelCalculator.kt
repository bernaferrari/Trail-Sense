package com.kylecorry.sol.science.oceanography.waterlevel

import com.kylecorry.sol.math.Range
import java.time.ZonedDateTime
import java.time.ZoneId
import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class PiecewiseWaterLevelCalculator(private val calculators: List<Pair<Range<ZonedDateTime>, IWaterLevelCalculator>>) :
    IWaterLevelCalculator {
    override fun calculate(instant: Instant, timeZone: TimeZone): Float {
        val time = instant.toJavaInstant().atZone(ZoneId.of(timeZone.id))
        return calculators.firstOrNull { it.first.contains(time) }?.second?.calculate(time) ?: 0f
    }
}
