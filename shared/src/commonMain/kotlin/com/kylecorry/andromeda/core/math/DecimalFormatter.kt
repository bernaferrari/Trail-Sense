package com.kylecorry.andromeda.core.math

expect object DecimalFormatter {
    fun format(number: Number, decimalPlaces: Int, strict: Boolean = true): String
}
