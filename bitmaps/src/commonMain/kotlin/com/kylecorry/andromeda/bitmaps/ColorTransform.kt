package com.kylecorry.andromeda.bitmaps

/** Maps each ARGB channel through its own 256-entry lookup table. */
fun PixelImage.lookup(
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
fun PixelImage.colorMatrix(matrix: FloatArray): PixelImage {
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
