package com.kylecorry.sol.units

public enum class EnergyUnits(public val id: Int, public val joules: Double, public val isMetric: Boolean) {
    Joules(1, 1.0, true),
    FootPounds(2, 1.355818, false),
}
