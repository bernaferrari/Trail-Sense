package com.kylecorry.andromeda.pdf

import com.kylecorry.andromeda.bitmaps.PixelImage

public data class PdfPageSize(val widthPoints: Float, val heightPoints: Float)

/** A PDF document opened from bytes. Page indices are zero-based. Close it when finished.
 * Android requires KSensor context initialization and stores a temporary copy in the app cache.
 */
public expect class PdfPages(bytes: ByteArray) : AutoCloseable {
    public val pageCount: Int
    public fun pageSize(page: Int): PdfPageSize
    /** Renders a page to the exact pixel dimensions, filling empty areas with [background]. */
    public fun render(page: Int, width: Int, height: Int, background: Int = -1): PixelImage
    /** Renders a rectangle in displayed-page fractions measured from the top left. */
    public fun renderRegion(
        page: Int, width: Int, height: Int,
        left: Double, top: Double, right: Double, bottom: Double, background: Int = -1
    ): PixelImage
    override fun close()
}

internal fun requireRenderSize(width: Int, height: Int) {
    require(width > 0 && height > 0 && width.toLong() * height <= Int.MAX_VALUE / 4) { "Invalid PDF render size" }
}

internal fun requirePageRegion(left: Double, top: Double, right: Double, bottom: Double) {
    require(left >= 0 && top >= 0 && right <= 1 && bottom <= 1 && right > left && bottom > top)
}
