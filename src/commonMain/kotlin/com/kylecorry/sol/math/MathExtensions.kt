package com.kylecorry.sol.math

import com.kylecorry.sol.math.arithmetic.Arithmetic
import com.kylecorry.sol.math.trigonometry.Trigonometry

public object MathExtensions {

    public fun Double.toRadians(): Double {
        return Trigonometry.toRadians(this)
    }

    public fun Float.toRadians(): Float {
        return Trigonometry.toRadians(this)
    }

    public fun Float.toDegrees(): Float {
        return Trigonometry.toDegrees(this)
    }

    public fun Double.toDegrees(): Double {
        return Trigonometry.toDegrees(this)
    }

    public fun Double.roundPlaces(places: Int): Double {
        return Arithmetic.roundPlaces(this, places)
    }

    public fun Float.roundPlaces(places: Int): Float {
        return Arithmetic.roundPlaces(this, places)
    }

    public fun Double.roundNearest(nearest: Double): Double {
        return Arithmetic.roundNearest(this, nearest)
    }

    public fun Float.roundNearest(nearest: Float): Float {
        return Arithmetic.roundNearest(this, nearest)
    }

    public fun Int.roundNearest(nearest: Int): Int {
        return Arithmetic.roundNearest(this, nearest)
    }

    public fun Double.roundNearestAngle(nearest: Double): Double {
        return Trigonometry.roundNearestAngle(this, nearest)
    }

    public fun Float.roundNearestAngle(nearest: Float): Float {
        return Trigonometry.roundNearestAngle(this, nearest)
    }

    public fun Float.real(defaultValue: Float = 0f): Float {
        return Arithmetic.real(this, defaultValue)
    }

    public fun Float.positive(zeroReplacement: Float = 0f): Float {
        return Arithmetic.positive(this, zeroReplacement)
    }

    public fun Float.negative(zeroReplacement: Float = 0f): Float {
        return Arithmetic.negative(this, zeroReplacement)
    }

    public fun Float.round(method: RoundingMethod): Int {
        return Arithmetic.round(this, method)
    }
}
