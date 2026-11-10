package com.kylecorry.andromeda.signal

public enum class CellNetwork(public val id: Int, public val minDbm: Int, public val maxDbm: Int) {
    Nr(1, -140, -44),
    Lte(2, -140, -44),
    Cdma(3, -100, -75),
    Wcdma(4, -113, -51),
    Gsm(5, -113, -51),
    Tdscdma(6, -120, -24)
}