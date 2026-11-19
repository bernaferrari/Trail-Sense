package com.kylecorry.trail_sense.tools.offline_maps.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.PdfRegion
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.SharedMapFiles
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.ceil
import kotlin.math.floor

internal const val PDF_TILE_EDGE = 512
private const val PDF_TILE_LIMIT = 16
private const val PDF_MAX_DETAIL = 32

internal data class PdfTile(val pageWidth: Int, val pageHeight: Int, val x: Int, val y: Int) {
    val width = minOf(PDF_TILE_EDGE, pageWidth - x * PDF_TILE_EDGE)
    val height = minOf(PDF_TILE_EDGE, pageHeight - y * PDF_TILE_EDGE)
    val region = PdfRegion(
        (x * PDF_TILE_EDGE).toDouble() / pageWidth,
        (y * PDF_TILE_EDGE).toDouble() / pageHeight,
        (x * PDF_TILE_EDGE + width).toDouble() / pageWidth,
        (y * PDF_TILE_EDGE + height).toDouble() / pageHeight
    )
}

/** Chooses a stable detail level and bounds the number of bitmaps needed for a single view. */
internal fun planPdfTiles(previewWidth: Int, previewHeight: Int, detail: Double, visible: PdfRegion): List<PdfTile> {
    if (!detail.isFinite() || detail <= 1.0 || previewWidth <= 0 || previewHeight <= 0) return emptyList()
    val left = visible.left.coerceIn(0.0, 1.0)
    val top = visible.top.coerceIn(0.0, 1.0)
    val right = visible.right.coerceIn(0.0, 1.0)
    val bottom = visible.bottom.coerceIn(0.0, 1.0)
    if (!(right > left && bottom > top)) return emptyList()
    var scale = 2
    while (scale < detail && scale < PDF_MAX_DETAIL) scale *= 2
    while (scale > 1) {
        val width = previewWidth * scale
        val height = previewHeight * scale
        val columns = floor(left * width / PDF_TILE_EDGE).toInt() until ceil(right * width / PDF_TILE_EDGE).toInt()
        val rows = floor(top * height / PDF_TILE_EDGE).toInt() until ceil(bottom * height / PDF_TILE_EDGE).toInt()
        val count = columns.count() * rows.count()
        if (count <= PDF_TILE_LIMIT) return rows.flatMap { y -> columns.map { x -> PdfTile(width, height, x, y) } }
        scale /= 2
    }
    return emptyList()
}

/** The viewer keeps its preview visible as detailed tiles arrive or the viewport changes. */
internal class PdfMapTiles(private val preview: ImageBitmap) {
    private val requests = MutableStateFlow(emptyList<PdfTile>())
    var visibleTiles by mutableStateOf<Map<PdfTile, ImageBitmap>>(emptyMap())
        private set

    fun update(detail: Double, region: PdfRegion) {
        requests.value = planPdfTiles(preview.width, preview.height, detail, region)
    }

    suspend fun load(files: SharedMapFiles, map: PhotoMap, invalidate: () -> Unit = {}) {
        val cache = LinkedHashMap<PdfTile, ImageBitmap>()
        try {
            requests.first { it.isNotEmpty() }
            files.withPdfPage(map) { page ->
                requests.collectLatest { tiles ->
                    val found = tiles.mapNotNull { tile ->
                        cache.remove(tile)?.also { cache[tile] = it }?.let { tile to it }
                    }.toMap()
                    visibleTiles = found
                    invalidate()
                    for (tile in tiles) {
                        if (tile in found) continue
                        val image = page.render(tile.region, tile.width, tile.height).decodeToImageBitmap()
                        if (cache.size >= PDF_TILE_LIMIT) cache.remove(cache.keys.first())
                        cache[tile] = image
                        visibleTiles = visibleTiles + (tile to image)
                        invalidate()
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // An unavailable or unreadable original leaves the stored preview usable.
        } finally {
            visibleTiles = emptyMap()
            invalidate()
        }
    }
}
