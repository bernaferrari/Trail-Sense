package com.kylecorry.andromeda.bitmaps

public class Rgba3dArray(public val values: ByteArray, public val sizeX: Int, public val sizeY: Int, public val sizeZ: Int) {
    init {
        require(sizeX > 0 && sizeY > 0 && sizeZ > 0)
        require(sizeX.toLong() * sizeY <= Int.MAX_VALUE / 4L / sizeZ)
        require(values.size.toLong() >= sizeX.toLong() * sizeY * sizeZ * 4)
    }

    public operator fun get(x: Int, y: Int, z: Int): ByteArray {
        val index = indexOfVector(x, y, z)
        return ByteArray(4) { values[index + it] }
    }

    public operator fun set(x: Int, y: Int, z: Int, value: ByteArray) {
        require(value.size == 4)
        val index = indexOfVector(x, y, z)
        for (i in 0..3) {
            values[index + i] = value[i]
        }
    }

    private fun indexOfVector(x: Int, y: Int, z: Int): Int {
        require(x in 0 until sizeX)
        require(y in 0 until sizeY)
        require(z in 0 until sizeZ)
        return ((z * sizeY + y) * sizeX + x) * 4
    }
}

