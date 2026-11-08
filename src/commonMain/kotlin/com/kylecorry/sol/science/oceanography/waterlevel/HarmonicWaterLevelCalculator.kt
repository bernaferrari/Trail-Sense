package com.kylecorry.sol.science.oceanography.waterlevel

import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.sol.science.oceanography.TidalHarmonic
import kotlinx.datetime.TimeZone
import kotlin.time.Instant
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class HarmonicWaterLevelCalculator(private val harmonics: List<TidalHarmonic>) :
    IWaterLevelCalculator {

    private class NodalCorrections(val day: Long, val f: DoubleArray, val u: DoubleArray)

    private val nodalCorrections = AtomicReference<NodalCorrections?>(null)

    override fun calculate(time: Instant, timeZone: TimeZone): Float {
        val nodal = getNodalCorrections(time)
        val astronomical = AstronomicalArgument.at(time.epochSeconds + time.nanosecondsOfSecond / 1e9)
        var height = 0.0
        for (i in harmonics.indices) {
            val harmonic = harmonics[i]
            val argument = astronomical.of(harmonic.constituent) ?: continue
            height += nodal.f[i] * harmonic.amplitude *
                    Trigonometry.cosDegrees(argument + nodal.u[i] - harmonic.phase)
        }
        return height.toFloat()
    }

    // The nodal terms change slowly, so they are only recomputed when the day changes
    private fun getNodalCorrections(time: Instant): NodalCorrections {
        val day = time.epochSeconds.floorDiv(SECONDS_PER_DAY)
        nodalCorrections.load()?.let { if (it.day == day) return it }

        val nodal = NodalCorrection.at(day * SECONDS_PER_DAY)
        val f = DoubleArray(harmonics.size)
        val u = DoubleArray(harmonics.size)
        for (i in harmonics.indices) {
            val (nodalF, nodalU) = nodal.of(harmonics[i].constituent)
            f[i] = nodalF
            u[i] = nodalU
        }
        return NodalCorrections(day, f, u).also { nodalCorrections.store(it) }
    }

    private companion object {
        const val SECONDS_PER_DAY = 86400L
    }

}
