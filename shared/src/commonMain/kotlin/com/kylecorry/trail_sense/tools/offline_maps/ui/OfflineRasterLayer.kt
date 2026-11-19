package com.kylecorry.trail_sense.tools.offline_maps.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.kylecorry.andromeda.core.units.PixelCoordinate
import androidx.compose.ui.graphics.Matrix
import com.kylecorry.andromeda.canvas.ImageMode
import com.kylecorry.sol.math.Vector2
import com.kylecorry.sol.math.geometry.Size
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.canvas.MapDrawer
import com.kylecorry.trail_sense.shared.canvas.setPolyToPoly
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.overlay.OverlayLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toPixel
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.toCoordinate
import com.kylecorry.trail_sense.tools.offline_maps.infrastructure.PdfRegion
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap

/** A calibrated preview, warped in patches so its projection can differ from the map camera's. */
internal class RasterMapOverlay(val map: PhotoMap, val image: ImageBitmap) {
    val bounds = imageBounds(map)
    val tiles = PdfMapTiles(image)
    val columns = minOf(GRID_SIZE, image.width)
    val rows = minOf(GRID_SIZE, image.height)
    val coordinates = Array(rows + 1) { y ->
        Array(columns + 1) { x ->
            map.imageProjection.toCoordinate(Vector2(
                (x * image.width / columns).toFloat(),
                (y * image.height / rows).toFloat()
            ))
        }
    }

    private companion object {
        const val GRID_SIZE = 8
    }
}

/** The stored landmarks are normalized. An unrotated map's bounds can be read before decoding its image. */
internal fun rasterPreviewBounds(map: PhotoMap): CoordinateBounds? {
    if (!map.visible || map.georeference.calibrationPoints.size < 2) return null
    if (map.georeference.rotation != 0f) return CoordinateBounds.world
    val unit = Size(1024f, 1024f)
    return runCatching {
        imageBounds(map.copy(georeference = map.georeference.copy(size = unit, imageSize = unit)))
    }.getOrNull()
}

private fun imageBounds(map: PhotoMap): CoordinateBounds? {
    val size = map.georeference.imageSize
    return CoordinateBounds.from(
        listOf(
            Vector2(0f, 0f), Vector2(size.width, 0f),
            Vector2(0f, size.height), Vector2(size.width, size.height)
        ).map(map.imageProjection::toCoordinate)
    )
}

internal class OfflineRasterLayer : OverlayLayer() {
    override val layerId = "offline_raster"
    var maps by mutableStateOf<List<RasterMapOverlay>>(emptyList())

    private val matrix = Matrix()
    private val source = FloatArray(8)
    private val destination = FloatArray(8)

    override fun drawOverlay(drawer: MapDrawer, map: IMapView) = Unit

    override fun draw(drawer: MapDrawer, map: IMapView) {
        if (maps.isEmpty()) return
        drawer.push()
        try {
            drawer.imageMode(ImageMode.Corner)
            drawer.noSmooth()
            for (raster in maps) {
                if (raster.bounds?.intersects(map.mapBounds) == false) continue
                val pixels = raster.coordinates.map { row -> row.map(map::toPixel) }
                val image = raster.image
                for (y in 0 until raster.rows) {
                    val top = y * image.height / raster.rows
                    val height = (y + 1) * image.height / raster.rows - top
                    for (x in 0 until raster.columns) {
                        val left = x * image.width / raster.columns
                        val width = (x + 1) * image.width / raster.columns - left
                        val p0 = pixels[y][x]
                        val p1 = pixels[y][x + 1]
                        val p2 = pixels[y + 1][x]
                        val p3 = pixels[y + 1][x + 1]
                        patch(drawer, image, left, top, width, height, p0, p1, p2, p3)
                    }
                }
                drawPdfTiles(drawer, map, raster)
            }
        } finally {
            drawer.pop()
        }
    }

    private fun drawPdfTiles(drawer: MapDrawer, view: IMapView, raster: RasterMapOverlay) {
        val projection = raster.map.imageProjection
        val width = raster.image.width
        val height = raster.image.height
        fun pixel(x: Float, y: Float) = view.toPixel(projection.toCoordinate(Vector2(x, y)))
        val center = projection.toPixels(view.mapCenter)
        val anchor = pixel(center.x, center.y)
        val detail = maxOf(
            anchor.distanceTo(pixel(center.x + 1, center.y)),
            anchor.distanceTo(pixel(center.x, center.y + 1))
        )
        if (detail <= 1f || !detail.isFinite()) {
            raster.tiles.update(0.0, PdfRegion())
            return
        }
        val corners = listOf(
            PixelCoordinate(0f, 0f), PixelCoordinate(drawer.width, 0f),
            PixelCoordinate(0f, drawer.height), PixelCoordinate(drawer.width, drawer.height)
        ).map { projection.toPixels(view.toCoordinate(it)) }
        raster.tiles.update(detail.toDouble(), PdfRegion(
            corners.minOf { it.x }.toDouble() / width, corners.minOf { it.y }.toDouble() / height,
            corners.maxOf { it.x }.toDouble() / width, corners.maxOf { it.y }.toDouble() / height
        ))
        for ((tile, image) in raster.tiles.visibleTiles) {
            val r = tile.region
            val left = (r.left * width).toFloat()
            val right = (r.right * width).toFloat()
            val top = (r.top * height).toFloat()
            val bottom = (r.bottom * height).toFloat()
            patch(drawer, image, 0, 0, image.width, image.height,
                pixel(left, top), pixel(right, top), pixel(left, bottom), pixel(right, bottom))
        }
    }

    private fun patch(
        drawer: MapDrawer, image: ImageBitmap, left: Int, top: Int, width: Int, height: Int,
        p0: PixelCoordinate, p1: PixelCoordinate, p2: PixelCoordinate, p3: PixelCoordinate
    ) {
        if (maxOf(p0.x, p1.x, p2.x, p3.x) < 0f || minOf(p0.x, p1.x, p2.x, p3.x) > drawer.width ||
            maxOf(p0.y, p1.y, p2.y, p3.y) < 0f || minOf(p0.y, p1.y, p2.y, p3.y) > drawer.height
        ) return
        source[2] = width.toFloat()
        source[5] = height.toFloat()
        source[6] = width.toFloat()
        source[7] = height.toFloat()
        destination[0] = p0.x
        destination[1] = p0.y
        destination[2] = p1.x
        destination[3] = p1.y
        destination[4] = p2.x
        destination[5] = p2.y
        destination[6] = p3.x
        destination[7] = p3.y
        if (matrix.setPolyToPoly(source, destination)) {
            drawer.withTransform(matrix) {
                drawer.image(image, left, top, width, height, 0f, 0f, width.toFloat(), height.toFloat())
            }
        }
    }
}
