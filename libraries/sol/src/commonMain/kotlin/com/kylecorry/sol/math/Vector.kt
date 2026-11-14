package com.kylecorry.sol.math

import kotlin.jvm.JvmInline

import com.kylecorry.sol.math.algebra.Matrix
import kotlin.math.sqrt

@JvmInline
public value class Vector(public val data: FloatArray) {

    public val n: Int
        get() = data.size

    public val size: Int
        get() = data.size

    public operator fun get(index: Int): Float {
        return data[index]
    }

    public operator fun set(index: Int, value: Float) {
        data[index] = value
    }

    public operator fun times(scalar: Float): Vector {
        return Vector(FloatArray(n) { i -> data[i] * scalar })
    }

    public operator fun plus(other: Vector): Vector {
        return Vector(FloatArray(n) { i -> data[i] + other.data[i] })
    }

    public operator fun minus(other: Vector): Vector {
        return Vector(FloatArray(n) { i -> data[i] - other.data[i] })
    }

    public fun magnitude(): Float {
        return norm()
    }

    public fun norm(): Float {
        return sqrt(data.sumOf { it.toDouble() * it }).toFloat()
    }

    public fun toColumnMatrix(): Matrix {
        return Matrix.column(values = data)
    }

    public fun toRowMatrix(): Matrix {
        return Matrix.row(values = data)
    }

    public companion object {
        public fun from(vararg values: Float): Vector {
            return Vector(values)
        }

        public fun create(size: Int): Vector {
            return Vector(FloatArray(size))
        }
    }
}
