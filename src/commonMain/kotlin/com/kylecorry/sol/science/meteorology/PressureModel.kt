package com.kylecorry.sol.science.meteorology

import com.kylecorry.sol.units.Pressure

internal object PressureModel {
    fun isHigh(pressure: Pressure): Boolean = pressure.hpa().value >= 1022.689
    fun isLow(pressure: Pressure): Boolean = pressure.hpa().value <= 1009.144

    fun tendency(last: Pressure, current: Pressure, seconds: Long, changeThreshold: Float): PressureTendency {
        val diff = current.hpa().value - last.hpa().value
        val dt = seconds

        if (dt == 0L) {
            return PressureTendency(PressureCharacteristic.Steady, 0f)
        }

        val changeAmt = (diff / dt) * 60 * 60

        val fastThreshold = changeThreshold + 2 / 3f

        val characteristic = when {
            changeAmt <= -fastThreshold -> PressureCharacteristic.FallingFast
            changeAmt <= -changeThreshold -> PressureCharacteristic.Falling
            changeAmt >= fastThreshold -> PressureCharacteristic.RisingFast
            changeAmt >= changeThreshold -> PressureCharacteristic.Rising
            else -> PressureCharacteristic.Steady
        }

        return PressureTendency(characteristic, changeAmt)

    }
}
