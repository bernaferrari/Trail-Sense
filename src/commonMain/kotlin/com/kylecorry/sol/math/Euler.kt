package com.kylecorry.sol.math

public data class Euler(val roll: Float, val pitch: Float, val yaw: Float) {
    public fun toFloatArray(): FloatArray {
        return floatArrayOf(roll, pitch, yaw)
    }

    public fun toQuaternion(): Quaternion {
        return Quaternion.from(this)
    }

    public companion object {
        public fun from(arr: FloatArray): Euler {
            return Euler(arr[0], arr[1], arr[2])
        }

        public fun from(quaternion: Quaternion): Euler {
            return quaternion.toEuler()
        }

    }

}
