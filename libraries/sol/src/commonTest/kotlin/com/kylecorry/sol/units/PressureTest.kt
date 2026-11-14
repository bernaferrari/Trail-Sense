package com.kylecorry.sol.units

import kotlin.test.Test
import kotlin.test.assertEquals
import com.kylecorry.sol.tests.arg

internal class PressureTest {

    @Test
    fun convertPressure() {
        providePressures().forEach { args -> convertPressure(args[0].arg<Float>(), args[1].arg<PressureUnits>(), args[2].arg<PressureUnits>(), args[3].arg<Float>()) }
    }

    private fun convertPressure(pressure: Float, from: PressureUnits, to: PressureUnits, expected: Float) {
        val p = Pressure.from(pressure, from).convertTo(to)
        assertEquals(expected, p.value, 0.01f)
        assertEquals(to, p.units)
    }

    companion object {
        fun providePressures(): List<List<Any?>> {
            return listOf(
                listOf(1000f, PressureUnits.Hpa, PressureUnits.Inhg, 29.53f),
                listOf(1000f, PressureUnits.Hpa, PressureUnits.Psi, 14.50377f),
                listOf(1000f, PressureUnits.Hpa, PressureUnits.Mbar, 1000f),
                listOf(1000f, PressureUnits.Hpa, PressureUnits.Hpa, 1000f),
                listOf(1000f, PressureUnits.Hpa, PressureUnits.MmHg, 750.06158f),

                listOf(1000f, PressureUnits.Mbar, PressureUnits.Hpa, 1000f),
                listOf(1000f, PressureUnits.Mbar, PressureUnits.Inhg, 29.53f),
                listOf(1000f, PressureUnits.Mbar, PressureUnits.Psi, 14.5037f),
                listOf(1000f, PressureUnits.Mbar, PressureUnits.Mbar, 1000f),
                listOf(1000f, PressureUnits.Mbar, PressureUnits.MmHg, 750.06158f),

                listOf(29.53f, PressureUnits.Inhg, PressureUnits.Hpa, 1000f),
                listOf(29.53f, PressureUnits.Inhg, PressureUnits.Psi, 14.5037f),
                listOf(29.53f, PressureUnits.Inhg, PressureUnits.Mbar, 1000f),
                listOf(29.53f, PressureUnits.Inhg, PressureUnits.Inhg, 29.53f),
                listOf(29.53f, PressureUnits.Inhg, PressureUnits.MmHg, 750.06158f),

                listOf(14.50377f, PressureUnits.Psi, PressureUnits.Hpa, 1000f),
                listOf(14.5037f, PressureUnits.Psi, PressureUnits.Inhg, 29.53f),
                listOf(14.50377f, PressureUnits.Psi, PressureUnits.Mbar, 1000f),
                listOf(14.5037f, PressureUnits.Psi, PressureUnits.Psi, 14.5037f),
                listOf(14.5037f, PressureUnits.Psi, PressureUnits.MmHg, 750.06158f),

                listOf(750.06158f, PressureUnits.MmHg, PressureUnits.Hpa, 1000f),
                listOf(750.06158f, PressureUnits.MmHg, PressureUnits.Inhg, 29.53f),
                listOf(750.06158f, PressureUnits.MmHg, PressureUnits.Mbar, 1000f),
                listOf(750.06158f, PressureUnits.MmHg, PressureUnits.Psi, 14.5037f),
                listOf(750.06158f, PressureUnits.MmHg, PressureUnits.MmHg, 750.06158f)
            )
        }
    }

}