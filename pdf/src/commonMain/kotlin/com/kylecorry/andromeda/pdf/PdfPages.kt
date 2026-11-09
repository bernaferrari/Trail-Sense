package com.kylecorry.andromeda.pdf

import com.kylecorry.andromeda.bitmaps.PixelImage

data class PdfPageSize(val widthPoints: Float, val heightPoints: Float)

/** A PDF document opened from bytes. Page indices are zero-based. Close it when finished.
 * Android requires KSensor context initialization and stores a temporary copy in the app cache.
 */
expect class PdfPages(bytes: ByteArray) : AutoCloseable {
    val pageCount: Int
    fun pageSize(page: Int): PdfPageSize
    /** Renders a page to the exact pixel dimensions, filling empty areas with [background]. */
    fun render(page: Int, width: Int, height: Int, background: Int = -1): PixelImage
    override fun close()
}

internal fun requireRenderSize(width: Int, height: Int) {
    require(width > 0 && height > 0 && width.toLong() * height <= Int.MAX_VALUE / 4) { "Invalid PDF render size" }
}
