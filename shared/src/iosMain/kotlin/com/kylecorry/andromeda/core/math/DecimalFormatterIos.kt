package com.kylecorry.andromeda.core.math

import com.kylecorry.sol.math.MathExtensions.roundPlaces
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

public actual object DecimalFormatter {
    public actual fun format(number: Number, decimalPlaces: Int, strict: Boolean): String {
        val value = number.toDouble()
        if (!value.isFinite()) return "-"
        val places = decimalPlaces.coerceAtLeast(0)
        val formatter = NSNumberFormatter().apply {
            numberStyle = NSNumberFormatterDecimalStyle
            usesGroupingSeparator = false
            minimumFractionDigits = if (strict) places.toULong() else 0uL
            maximumFractionDigits = places.toULong()
        }
        return formatter.stringFromNumber(NSNumber(value.roundPlaces(decimalPlaces))) ?: "-"
    }
}
