package com.kylecorry.andromeda.core.math

public expect object DecimalFormatter {
    public fun format(number: Number, decimalPlaces: Int, strict: Boolean = true): String
}
