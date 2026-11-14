package com.kylecorry.sol.science.meteorology

public data class PressureTendency(val characteristic: PressureCharacteristic, val amount: Float) {
    public companion object {
        public val zero: PressureTendency = PressureTendency(PressureCharacteristic.Steady, 0f)
    }
}
