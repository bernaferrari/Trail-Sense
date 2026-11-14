package com.kylecorry.sol.math.geometry

import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.algebra.LinearEquation
import com.kylecorry.sol.math.arithmetic.Arithmetic

public data class Line(val start: Vector2, val end: Vector2) {

    val isVertical: Boolean
        get() = Arithmetic.isApproximatelyEqual(start.x, end.x) || slope().isInfinite()
    val isHorizontal: Boolean
        get() {
            val m = slope()
            return Arithmetic.isApproximatelyEqual(start.y, end.y) || Arithmetic.isZero(m) || m.isNaN()
        }

    public fun left(): Vector2 {
        return if (start.x < end.x) {
            start
        } else {
            end
        }
    }

    public fun right(): Vector2 {
        return if (start.x < end.x) {
            end
        } else {
            start
        }
    }

    public fun top(): Vector2 {
        return if (start.y < end.y) {
            end
        } else {
            start
        }
    }

    public fun bottom(): Vector2 {
        return if (start.y < end.y) {
            start
        } else {
            end
        }
    }

    public fun length(): Float {
        return start.distanceTo(end)
    }

    /**
     * The slope of the line
     */
    public fun slope(): Float {
        return (end.y - start.y) / (end.x - start.x)
    }

    /**
     * The Y intercept
     */
    public fun intercept(): Float {
        return start.y - slope() * start.x
    }

    public fun equation(): LinearEquation {
        return LinearEquation(slope(), intercept())
    }
}
