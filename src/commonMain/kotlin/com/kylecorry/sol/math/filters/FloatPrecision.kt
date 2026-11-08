package com.kylecorry.sol.math.filters

import kotlin.math.abs

/** Distance to the next representable float, including subnormal and maximum values. */
internal fun floatUlp(value: Float): Float {
    val magnitude = abs(value)
    if (!magnitude.isFinite()) return magnitude
    val bits = magnitude.toBits()
    return if (bits == 0x7f7fffff) magnitude - Float.fromBits(bits - 1)
    else Float.fromBits(bits + 1) - magnitude
}
