package com.kylecorry.sol.math.arithmetic

import com.kylecorry.sol.math.RoundingMethod
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sign

public object Arithmetic {

    public const val EPSILON_FLOAT: Float = 1e-5f
    public const val EPSILON_DOUBLE: Double = 1e-10

    public fun factorial(n: Int): Long {
        if (n == 0) {
            return 1
        }
        var result = 1L
        for (i in 2..n.absoluteValue) {
            result *= i
        }
        return result * n.sign
    }

    public fun wrap(value: Float, min: Float, max: Float): Float {
        return wrap(value.toDouble(), min.toDouble(), max.toDouble()).toFloat()
    }

    public fun wrap(value: Double, min: Double, max: Double): Double {
        // https://stackoverflow.com/questions/14415753/wrap-value-into-range-min-max-without-division
        val range = max - min
        if (value < min) {
            return max - (min - value) % range
        }

        if (value > max) {
            return min + (value - min) % range
        }

        return value
    }

    public fun power(x: Int, power: Int): Int {
        if (x == 1) {
            return 1
        }

        if (power < 0) {
            return 0
        }


        var total = 1
        repeat(power) {
            total *= x
        }

        return total
    }

    public fun power(x: Double, power: Int): Double {
        var total = 1.0
        repeat(abs(power)) {
            total *= x
        }

        if (power < 0) {
            return 1 / total
        }

        return total
    }

    public fun power(x: Float, power: Int): Float {
        var total = 1f
        repeat(abs(power)) {
            total *= x
        }

        if (power < 0) {
            return 1 / total
        }

        return total
    }

    public fun cube(a: Double): Double {
        return a * a * a
    }

    public fun square(a: Double): Double {
        return a * a
    }

    public fun cube(a: Float): Float {
        return a * a * a
    }

    public fun square(a: Float): Float {
        return a * a
    }

    public fun clamp(value: Double, minimum: Double, maximum: Double): Double {
        return value.coerceIn(minimum, maximum)
    }

    public fun clamp(value: Float, minimum: Float, maximum: Float): Float {
        return value.coerceIn(minimum, maximum)
    }

    public fun isCloseTo(a: Double, b: Double, tolerance: Double): Boolean {
        return (a - b).absoluteValue <= tolerance
    }

    public fun isCloseTo(a: Float, b: Float, tolerance: Float): Boolean {
        return (a - b).absoluteValue <= tolerance
    }

    public fun greatestCommonDivisor(a: Long, b: Long): Long {
        val maxIterations = 1000
        var currentA = a
        var currentB = b
        var iterations = 0
        while (currentB != 0L && iterations < maxIterations) {
            val temp = currentB
            currentB = currentA % currentB
            currentA = temp
            iterations++
        }
        return currentA
    }

    public fun greatestCommonDivisor(a: Double, b: Double, precision: Double = 0.0001): Double {
        val maxIterations = 1000
        var currentA = a
        var currentB = b
        var iterations = 0
        while (currentB.absoluteValue > precision && iterations < maxIterations) {
            val temp = currentB
            currentB = currentA % currentB
            currentA = temp
            iterations++
        }
        return currentA
    }

    public fun greatestCommonDivisor(a: Float, b: Float): Float {
        return greatestCommonDivisor(a.toDouble(), b.toDouble()).toFloat()
    }

    public fun greatestCommonDivisor(a: Int, b: Int): Int {
        return greatestCommonDivisor(a.toLong(), b.toLong()).toInt()
    }

    public fun leastCommonMultiple(a: Long, b: Long): Long {
        if (a == 0L || b == 0L) {
            return 0
        }
        return abs(a) * (abs(b) / greatestCommonDivisor(a, b))
    }

    public fun leastCommonMultiple(a: Int, b: Int): Int {
        return leastCommonMultiple(a.toLong(), b.toLong()).toInt()
    }

    public fun leastCommonMultiple(a: Double, b: Double): Double {
        if (a == 0.0 || b == 0.0) {
            return 0.0
        }
        return abs(a) * (abs(b) / greatestCommonDivisor(a, b))
    }

    public fun leastCommonMultiple(a: Float, b: Float): Float {
        return leastCommonMultiple(a.toDouble(), b.toDouble()).toFloat()
    }

    public fun isApproximatelyEqual(a: Float, b: Float, tolerance: Float = EPSILON_FLOAT): Boolean {
        return isZero(a - b, tolerance)
    }

    public fun isZero(value: Float, tolerance: Float = EPSILON_FLOAT): Boolean {
        return abs(value) < tolerance
    }

    /**
     * Rounds half up to the given number of decimal places. Values too large to have a fractional
     * part at that precision are returned unchanged instead of overflowing.
     */
    public fun roundPlaces(value: Double, places: Int): Double {
        val factor = 10.0.pow(places)
        val scaled = value * factor
        if (!scaled.isFinite() || abs(scaled) >= MAX_EXACT_DOUBLE) {
            return value
        }
        return floor(scaled + 0.5) / factor
    }

    public fun roundPlaces(value: Float, places: Int): Float {
        val factor = 10f.pow(places)
        val scaled = value * factor
        if (!scaled.isFinite() || abs(scaled) >= MAX_EXACT_FLOAT) {
            return value
        }
        return floor(scaled + 0.5f) / factor
    }

    private const val MAX_EXACT_DOUBLE = 4503599627370496.0 // 2^52
    private const val MAX_EXACT_FLOAT = 8388608f // 2^23

    public fun roundNearest(value: Double, nearest: Double): Double {
        return (value / nearest).roundToLong() * nearest
    }

    public fun roundNearest(value: Float, nearest: Float): Float {
        return (value / nearest).roundToLong() * nearest
    }

    public fun roundNearest(value: Int, nearest: Int): Int {
        return (value.toDouble() / nearest).roundToInt() * nearest
    }

    public fun round(value: Float, method: RoundingMethod): Int {
        return when (method) {
            RoundingMethod.AwayFromZero -> {
                if (abs(value) % 1 >= 0.5f) {
                    (sign(value) * abs(value).roundToInt()).toInt()
                } else {
                    value.toInt()
                }
            }

            RoundingMethod.TowardZero -> {
                if (abs(value) % 1 <= 0.5f) {
                    value.toInt()
                } else {
                    value.roundToInt()
                }
            }
        }
    }

    public fun real(value: Float, defaultValue: Float = 0f): Float {
        return if (value.isNaN() || value.isInfinite()) defaultValue else value
    }

    public fun positive(value: Float, zeroReplacement: Float = 0f): Float {
        return if (value < 0) {
            -value
        } else if (isZero(value)) {
            zeroReplacement
        } else {
            value
        }
    }

    public fun negative(value: Float, zeroReplacement: Float = 0f): Float {
        return if (value > 0) {
            -value
        } else if (isZero(value)) {
            zeroReplacement
        } else {
            value
        }
    }
}
