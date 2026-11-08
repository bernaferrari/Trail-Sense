package com.kylecorry.sol.science.oceanography.waterlevel

import com.kylecorry.sol.math.MathExtensions.toRadians
import com.kylecorry.sol.math.trigonometry.CosineWave
import com.kylecorry.sol.math.trigonometry.Waveform
import com.kylecorry.sol.science.oceanography.Tide
import com.kylecorry.sol.science.oceanography.TideConstituent
import com.kylecorry.sol.time.Time.hoursBetween
import java.time.ZonedDateTime
import java.time.ZoneId
import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class TideClockWaterLevelCalculator(
    private val reference: Tide,
    private val frequency: Float = TideConstituent.M2.speed,
    private val amplitude: Float = 1f,
    private val z0: Float = 0f
) : IWaterLevelCalculator {

    private val wave = getWave()

    override fun calculate(instant: Instant, timeZone: TimeZone): Float {
        val time = instant.toJavaInstant().atZone(ZoneId.of(timeZone.id))
        val t = hoursBetween(reference.time, time)
        return wave.calculate(t)
    }

    private fun getWave(): Waveform {
        val amplitude = if (!reference.isHigh) -amplitude else amplitude
        return CosineWave(amplitude, frequency.toRadians(), 0f, z0)
    }

}
