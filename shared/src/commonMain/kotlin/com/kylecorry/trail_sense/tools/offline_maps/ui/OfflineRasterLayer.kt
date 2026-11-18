package com.kylecorry.trail_sense.tools.offline_maps.ui

import androidx.compose.ui.graphics.ImageBitmap
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
import com.kylecorry.trail_sense.tools.offline_maps.domain.photo_maps.PhotoMap

/** A calibrated preview, warped in patches so its projection can differ from the map camera's. */
internal class RasterMapOverlay(val map: PhotoMap, val image: ImageBitmap) {
    val bounds = imageBounds(map)
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
    var maps = emptyList<RasterMapOverlay>()

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
                        if (maxOf(p0.x, p1.x, p2.x, p3.x) < 0f || minOf(p0.x, p1.x, p2.x, p3.x) > drawer.width ||
                            maxOf(p0.y, p1.y, p2.y, p3.y) < 0f || minOf(p0.y, p1.y, p2.y, p3.y) > drawer.height
                        ) continue
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
            }
        } finally {
            drawer.pop()
        }
    }
}
