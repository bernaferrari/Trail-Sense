package com.kylecorry.sol.science.astronomy.moon

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

internal class MoonTruePhaseTest {

    @Test
    fun anglesAreCorrect() {
        provideAngles().forEach { args -> anglesAreCorrect(args[0].arg<MoonTruePhase>(), args[1].arg<Float>(), args[2].arg<Float>()) }
    }

    private fun anglesAreCorrect(phase: MoonTruePhase, start: Float, end: Float){
        assertEquals(start, phase.startAngle, 0.001f)
        assertEquals(end, phase.endAngle, 0.001f)
    }

    companion object {
        fun provideAngles(): List<List<Any?>> {
            return listOf(
                listOf(MoonTruePhase.New, 348.75f, 11.25f),
                listOf(MoonTruePhase.WaningCrescent, 11.25f, 78.75f),
                listOf(MoonTruePhase.ThirdQuarter, 78.75f, 101.25f),
                listOf(MoonTruePhase.WaningGibbous, 101.25f, 168.75f),
                listOf(MoonTruePhase.Full, 168.75f, 191.25f),
                listOf(MoonTruePhase.WaxingGibbous, 191.25f, 258.75f),
                listOf(MoonTruePhase.FirstQuarter, 258.75f, 281.25f),
                listOf(MoonTruePhase.WaxingCrescent, 281.25f, 348.75f),
            )
        }
    }

}