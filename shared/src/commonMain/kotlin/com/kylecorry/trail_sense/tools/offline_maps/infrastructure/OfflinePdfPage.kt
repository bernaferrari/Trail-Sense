package com.kylecorry.trail_sense.tools.offline_maps.infrastructure

/** Coordinates are normalized to the displayed PDF page, with the origin at the top left. */
internal data class PdfRegion(
    val left: Double = 0.0,
    val top: Double = 0.0,
    val right: Double = 1.0,
    val bottom: Double = 1.0
)

internal interface OfflinePdfPage {
    val width: Double
    val height: Double
    suspend fun render(region: PdfRegion, width: Int, height: Int): ByteArray
    suspend fun close()
}

internal typealias OpenOfflinePdfPage = suspend (ByteArray, Int) -> OfflinePdfPage
