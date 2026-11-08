package com.kylecorry.sol.shared

import com.kylecorry.sol.math.MathExtensions.roundPlaces
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter

internal actual object DecimalFormatter {
    actual fun format(number: Number, decimalPlaces: Int, strict: Boolean): String {
        val value = number.toDouble()
        if (!value.isFinite()) return "-"
        val formatter = NSNumberFormatter()
        formatter.minimumIntegerDigits = if (strict || decimalPlaces <= 0) 1u else 0u
        formatter.minimumFractionDigits = if (strict) decimalPlaces.coerceAtLeast(0).toULong() else 0u
        formatter.maximumFractionDigits = decimalPlaces.coerceAtLeast(0).toULong()
        formatter.usesGroupingSeparator = false
        return formatter.stringFromNumber(NSNumber(value.roundPlaces(decimalPlaces))) ?: "-"
    }
}
