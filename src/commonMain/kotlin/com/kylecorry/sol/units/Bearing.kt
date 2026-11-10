package com.kylecorry.sol.units

import kotlin.jvm.JvmInline

import com.kylecorry.sol.math.trigonometry.Trigonometry.normalizeAngle

@JvmInline
public value class Bearing private constructor(public val value: Float) {
    public val direction: CompassDirection
        get() = CompassDirection.nearest(value)

    public val mils: Float
        get() = value * 17.77778f

    public fun withDeclination(declination: Float): Bearing {
        return from(value + declination)
    }

    public fun inverse(): Bearing {
        return from(value + 180)
    }

    public companion object {
        public fun from(direction: CompassDirection): Bearing {
            return from(direction.azimuth)
        }

        public fun from(value: Float): Bearing {
            return Bearing(if (value.isNaN() || !value.isFinite()) 0f else normalizeAngle(value))
        }

        public fun getBearing(degrees: Float): Float {
            return if (degrees.isNaN() || !degrees.isFinite()) 0f else normalizeAngle(degrees)
        }
    }
}
