package com.kylecorry.andromeda.bitmaps

/** Maps each ARGB channel through its own 256-entry lookup table. */
public fun PixelImage.lookup(
    red: ByteArray = identityLookup(),
    green: ByteArray = identityLookup(),
    blue: ByteArray = identityLookup(),
    alpha: ByteArray = identityLookup(),
): PixelImage {
    require(red.size == 256 && green.size == 256 && blue.size == 256 && alpha.size == 256)
    return map { color ->
        ((alpha[(color ushr 24) and 255].toInt() and 255) shl 24) or
            ((red[(color ushr 16) and 255].toInt() and 255) shl 16) or
            ((green[(color ushr 8) and 255].toInt() and 255) shl 8) or
            (blue[color and 255].toInt() and 255)
    }
}

private fun identityLookup() = ByteArray(256) { it.toByte() }

/** Applies a row-major 4 by 5 RGBA color matrix, including its per-channel offsets. */
public fun PixelImage.colorMatrix(matrix: FloatArray): PixelImage {
    require(matrix.size == 20 && matrix.all { it.isFinite() })
    return map { color ->
        val channels = intArrayOf((color ushr 16) and 255, (color ushr 8) and 255, color and 255, (color ushr 24) and 255)
        var result = 0
        for (row in 0..3) {
            val offset = row * 5
            var value = matrix[offset + 4]
            for (column in 0..3) value += matrix[offset + column] * channels[column]
            val shift = when (row) { 0 -> 16; 1 -> 8; 2 -> 0; else -> 24 }
            result = result or (value.toInt().coerceIn(0, 255) shl shift)
        }
        result
    }
}

/** Applies a reusable per-channel lookup table. */
public fun PixelImage.lookup(table: LookupTable): PixelImage = lookup(table.red, table.green, table.blue, table.alpha)

/** Applies a trilinearly interpolated RGB color cube, retaining each input pixel's alpha. */
public fun PixelImage.lookup3d(cube: Rgba3dArray): PixelImage {
    require(cube.sizeX in 2..256 && cube.sizeY in 2..256 && cube.sizeZ in 2..256)
    return map { color ->
        val x = ((color ushr 16) and 255) * (cube.sizeX - 1) / 255f
        val y = ((color ushr 8) and 255) * (cube.sizeY - 1) / 255f
        val z = (color and 255) * (cube.sizeZ - 1) / 255f
        val x0 = x.toInt()
        val y0 = y.toInt()
        val z0 = z.toInt()
        val x1 = (x0 + 1).coerceAtMost(cube.sizeX - 1)
        val y1 = (y0 + 1).coerceAtMost(cube.sizeY - 1)
        val z1 = (z0 + 1).coerceAtMost(cube.sizeZ - 1)
        val dx = x - x0
        val dy = y - y0
        val dz = z - z0
        fun sample(cx: Int, cy: Int, cz: Int, channel: Int): Float {
            val offset = ((cz * cube.sizeY + cy) * cube.sizeX + cx) * 4 + channel
            return (cube.values[offset].toInt() and 255).toFloat()
        }
        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
        var result = color and -0x1000000
        for (channel in 0..2) {
            val c00 = lerp(sample(x0, y0, z0, channel), sample(x1, y0, z0, channel), dx)
            val c10 = lerp(sample(x0, y1, z0, channel), sample(x1, y1, z0, channel), dx)
            val c01 = lerp(sample(x0, y0, z1, channel), sample(x1, y0, z1, channel), dx)
            val c11 = lerp(sample(x0, y1, z1, channel), sample(x1, y1, z1, channel), dx)
            val value = (lerp(lerp(c00, c10, dy), lerp(c01, c11, dy), dz) + 0.5f).toInt().coerceIn(0, 255)
            result = result or (value shl (16 - channel * 8))
        }
        result
    }
}
