package com.kylecorry.sol.science.oceanography.waterlevel

import com.kylecorry.sol.science.oceanography.TidalHarmonic
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.units.Coordinate
import kotlin.time.Duration
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

public class HarmonicLunitidalWaterLevelCalculator(
    lunitidalInterval: Duration,
    location: Coordinate = Coordinate.zero
) : IWaterLevelCalculator {

    private val harmonic = TidalHarmonic(
        TideConstituent.M2,
        1f,
        ((lunitidalInterval.inWholeMilliseconds / 3600000.0 - location.longitude / 15) *
            TideConstituent.M2.speed).toFloat()
    )

    private val harmonicCalculator = HarmonicWaterLevelCalculator(listOf(harmonic))

    override fun calculate(time: Instant, timeZone: TimeZone): Float {
        return harmonicCalculator.calculate(time, timeZone)
    }
}
