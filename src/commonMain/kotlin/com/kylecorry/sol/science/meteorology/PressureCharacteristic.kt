package com.kylecorry.sol.science.meteorology

public enum class PressureCharacteristic(
    public val isFalling: Boolean,
    public val isRising: Boolean,
    public val isRapid: Boolean
) {
    Falling(true, false, false),
    FallingFast(true, false, true),
    Rising(false, true, false),
    RisingFast(false, true, true),
    Steady(false, false, false)
}
