package com.kylecorry.sol.science.oceanography.waterlevel

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.science.oceanography.Tide
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

public class GapWaterLevelCalculator(
    private val first: Tide,
    private val second: Tide,
    private val approximateFrequency: Float
) :
    IWaterLevelCalculator {

    private val wave by lazy {
        val firstVec = Vector2(getX(first.time), first.height ?: (if (first.isHigh) 1f else -1f))
        val secondVec =
            Vector2(getX(second.time), second.height ?: (if (second.isHigh) 1f else -1f))
        Trigonometry.connect(firstVec, secondVec, approximateFrequency)
    }

    override fun calculate(time: Instant, timeZone: TimeZone): Float {
        return wave.calculate(getX(time))
    }

    private fun getX(time: Instant): Float {
        return hoursBetween(first.time, time)
    }

}
