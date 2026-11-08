package com.kylecorry.andromeda.bitmaps

/** Converts an NV21 or Android-aligned YV12 frame to opaque ARGB using limited-range BT.601. */
fun ByteArray.yuvToPixelImage(width: Int, height: Int, format: YuvFormat): PixelImage {
    require(width > 0 && height > 0 && width % 2 == 0 && height % 2 == 0)
    val yStride = if (format == YuvFormat.YV12) (width.toLong() + 15) / 16 * 16 else width.toLong()
    val chromaStride = if (format == YuvFormat.YV12) (yStride / 2 + 15) / 16 * 16 else width.toLong()
    val ySize = yStride * height
    val chromaSize = chromaStride * (height / 2)
    val total = ySize + if (format == YuvFormat.YV12) chromaSize * 2 else chromaSize
    require(width.toLong() * height <= Int.MAX_VALUE && total <= size.toLong()) { "YUV buffer is too small" }
    val output = IntArray(width * height)
    for (y in 0 until height) for (x in 0 until width) {
        val yValue = (this[(y * yStride + x).toInt()].toInt() and 255) - 16
        val chromaIndex = ySize + (y / 2) * chromaStride
        val vIndex = if (format == YuvFormat.NV21) chromaIndex + (x / 2) * 2 else chromaIndex + x / 2
        val uIndex = if (format == YuvFormat.NV21) vIndex + 1 else vIndex + chromaSize
        val u = (this[uIndex.toInt()].toInt() and 255) - 128
        val v = (this[vIndex.toInt()].toInt() and 255) - 128
        val red = ((298 * yValue + 409 * v + 128) shr 8).coerceIn(0, 255)
        val green = ((298 * yValue - 100 * u - 208 * v + 128) shr 8).coerceIn(0, 255)
        val blue = ((298 * yValue + 516 * u + 128) shr 8).coerceIn(0, 255)
        output[y * width + x] = -0x1000000 or (red shl 16) or (green shl 8) or blue
    }
    return PixelImage(width, height, output)
}
