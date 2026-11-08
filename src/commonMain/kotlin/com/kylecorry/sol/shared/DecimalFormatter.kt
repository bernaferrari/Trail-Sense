package com.kylecorry.sol.shared

internal expect object DecimalFormatter {
    fun format(number: Number, decimalPlaces: Int, strict: Boolean = true): String
}
