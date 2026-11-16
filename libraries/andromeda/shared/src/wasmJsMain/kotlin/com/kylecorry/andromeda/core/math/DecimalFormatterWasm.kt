package com.kylecorry.andromeda.core.math

import com.kylecorry.sol.math.MathExtensions.roundPlaces
import kotlin.math.abs
import kotlin.math.roundToLong

public actual object DecimalFormatter {
    public actual fun format(number: Number, decimalPlaces: Int, strict: Boolean): String {
        val value = number.toDouble()
        if (!value.isFinite()) return "-"
        val places = decimalPlaces.coerceAtLeast(0)
        val rounded = value.roundPlaces(places)
        if (!rounded.isFinite()) return "-"
        if (places == 0) {
            if (abs(rounded) < 9e15) {
                val whole = rounded.roundToLong()
                return if (whole == 0L) "0" else whole.toString()
            }
            return rounded.toString()
        }

        if (places <= 12 && abs(rounded) < 1e6) {
            var scale = 1L
            repeat(places) { scale *= 10L }
            val scaled = (abs(rounded) * scale).roundToLong()
            if (scaled == 0L) {
                return if (strict) "0." + "0".repeat(places) else "0"
            }
            val sign = if (rounded < 0.0) "-" else ""
            val integerPart = scaled / scale
            val fractionDigits = (scaled % scale).toString().padStart(places, '0')
            val trimmedFraction = if (strict) fractionDigits else fractionDigits.trimEnd('0')
            return if (trimmedFraction.isEmpty()) {
                "$sign$integerPart"
            } else {
                "$sign$integerPart.$trimmedFraction"
            }
        }

        val raw = rounded.toString()
        if ('e' in raw || 'E' in raw) return raw
        val dot = raw.indexOf('.')
        if (dot < 0) {
            return if (strict) "$raw.${"0".repeat(places)}" else raw
        }
        val intPart = raw.substring(0, dot)
        val fracPart = raw.substring(dot + 1).take(places)
        val padded = if (strict) fracPart.padEnd(places, '0') else fracPart.trimEnd('0')
        return if (padded.isEmpty()) intPart else "$intPart.$padded"
    }
}
