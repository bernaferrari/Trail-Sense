package com.kylecorry.sol.math

import kotlin.math.sqrt

public data class Vector3Precise(val x: Double, val y: Double, val z: Double) {

    private val array = doubleArrayOf(x, y, z)

    public fun cross(other: Vector3Precise): Vector3Precise {
        val arr = Vector3PreciseUtils.cross(toDoubleArray(), other.toDoubleArray())
        return Vector3Precise(arr[0], arr[1], arr[2])
    }

    public operator fun minus(other: Vector3Precise): Vector3Precise {
        val arr = Vector3PreciseUtils.minus(toDoubleArray(), other.toDoubleArray())
        return Vector3Precise(arr[0], arr[1], arr[2])
    }

    public operator fun plus(other: Vector3Precise): Vector3Precise {
        val arr = Vector3PreciseUtils.plus(toDoubleArray(), other.toDoubleArray())
        return Vector3Precise(arr[0], arr[1], arr[2])
    }

    public operator fun times(factor: Double): Vector3Precise {
        val arr = Vector3PreciseUtils.times(toDoubleArray(), factor)
        return Vector3Precise(arr[0], arr[1], arr[2])
    }

    public fun toDoubleArray(): DoubleArray {
        return array
    }

    public fun dot(other: Vector3Precise): Double {
        return Vector3PreciseUtils.dot(toDoubleArray(), other.toDoubleArray())
    }

    public fun magnitude(): Double {
        return Vector3PreciseUtils.magnitude(toDoubleArray())
    }

    public fun normalize(): Vector3Precise {
        val arr = Vector3PreciseUtils.normalize(toDoubleArray())
        return Vector3Precise(arr[0], arr[1], arr[2])
    }

    public companion object {
        public val zero: Vector3Precise = Vector3Precise(0.0, 0.0, 0.0)

        public fun from(arr: DoubleArray): Vector3Precise {
            return Vector3Precise(arr[0], arr[1], arr[2])
        }

    }

}


public object Vector3PreciseUtils {
    public fun cross(first: DoubleArray, second: DoubleArray): DoubleArray {
        return doubleArrayOf(
            first[1] * second[2] - first[2] * second[1],
            first[2] * second[0] - first[0] * second[2],
            first[0] * second[1] - first[1] * second[0]
        )
    }

    public fun minus(first: DoubleArray, second: DoubleArray): DoubleArray {
        return doubleArrayOf(
            first[0] - second[0],
            first[1] - second[1],
            first[2] - second[2]
        )
    }

    public fun project(first: DoubleArray, second: DoubleArray): DoubleArray {
        val mag = magnitude(second)
        return times(second, dot(first, second) / (mag * mag))
    }

    public fun projectOnPlane(first: DoubleArray, planeNormal: DoubleArray): DoubleArray {
        return minus(first, project(first, planeNormal))
    }

    public fun plus(first: DoubleArray, second: DoubleArray): DoubleArray {
        return doubleArrayOf(
            first[0] + second[0],
            first[1] + second[1],
            first[2] + second[2]
        )
    }

    public fun times(arr: DoubleArray, factor: Double): DoubleArray {
        return doubleArrayOf(
            arr[0] * factor,
            arr[1] * factor,
            arr[2] * factor
        )
    }

    public fun dot(first: DoubleArray, second: DoubleArray): Double {
        return first[0] * second[0] + first[1] * second[1] + first[2] * second[2]
    }

    public fun magnitude(arr: DoubleArray): Double {
        return sqrt(arr[0] * arr[0] + arr[1] * arr[1] + arr[2] * arr[2])
    }

    public fun normalize(arr: DoubleArray, inPlace: Boolean = false): DoubleArray {
        val mag = magnitude(arr)

        val newArr = if (inPlace) arr else DoubleArray(3)
        newArr[0] = arr[0] / mag
        newArr[1] = arr[1] / mag
        newArr[2] = arr[2] / mag

        return newArr
    }
}
