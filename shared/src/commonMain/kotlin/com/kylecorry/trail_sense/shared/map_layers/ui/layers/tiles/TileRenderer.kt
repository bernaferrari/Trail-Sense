package com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.canvas.MapDrawer
import com.kylecorry.trail_sense.shared.canvas.setPolyToPoly
import com.kylecorry.trail_sense.shared.map_layers.tiles.SharedImageTile
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import kotlin.math.max
import kotlin.math.min

/** Projects, clips, and draws tiles through the common map canvas. */
class TileRenderer(private val borderPixels: Int, private val maxTiles: Int) {
    private val renderMatrix = Matrix()
    private val srcPoints = FloatArray(8)
    private val dstPoints = FloatArray(8)
    private val clipPath = Path()
    private var renderedProjection: IMapViewProjection? = null
    private val projectedTiles = mutableMapOf<Tile, Array<PixelCoordinate>>()
    var hasFadingTiles = false
        private set

    fun prepare(projection: IMapViewProjection) {
        if (projection !== renderedProjection || projectedTiles.size > maxTiles * 2) {
            projectedTiles.clear()
            renderedProjection = projection
        }
        hasFadingTiles = false
    }

    fun clear() {
        projectedTiles.clear()
        renderedProjection = null
    }

    fun render(
        tile: SharedImageTile<*>,
        bitmap: ImageBitmap,
        drawer: MapDrawer,
        projection: IMapViewProjection,
        clipTo: Tile?
    ) {
        if (clipTo == null) renderTile(tile, drawer, projection, bitmap)
        else renderTileClipped(tile, drawer, projection, bitmap, clipTo)
    }

    private fun isTooSmall(
        topLeft: PixelCoordinate,
        topRight: PixelCoordinate,
        bottomLeft: PixelCoordinate,
        bottomRight: PixelCoordinate
    ): Boolean {
        val minSize = 10f
        val width = max(topRight.x, bottomRight.x) - min(topLeft.x, bottomLeft.x)
        val height = max(bottomLeft.y, bottomRight.y) - min(topLeft.y, topRight.y)
        return width < minSize || height < minSize
    }

    private fun isFarOffScreen(
        topLeft: PixelCoordinate,
        topRight: PixelCoordinate,
        bottomLeft: PixelCoordinate,
        bottomRight: PixelCoordinate,
        canvasWidth: Float,
        canvasHeight: Float
    ): Boolean {
        val buffer = canvasHeight.coerceAtLeast(canvasWidth) * 2
        val minX = minOf(topLeft.x, bottomLeft.x)
        val maxX = maxOf(topRight.x, bottomRight.x)
        val minY = minOf(topLeft.y, topRight.y)
        val maxY = maxOf(bottomLeft.y, bottomRight.y)

        return maxX < -buffer ||
                minX > canvasWidth + buffer ||
                maxY < -buffer ||
                minY > canvasHeight + buffer
    }

    private fun renderTile(
        imageTile: SharedImageTile<*>,
        drawer: MapDrawer,
        projection: IMapViewProjection,
        bitmap: ImageBitmap
    ) {
        val tile = imageTile.tile
        val corners = getProjectedCorners(tile, projection)
        val topLeftPixel = corners[0]
        val topRightPixel = corners[1]
        val bottomRightPixel = corners[2]
        val bottomLeftPixel = corners[3]

        if (isTooSmall(topLeftPixel, topRightPixel, bottomLeftPixel, bottomRightPixel)) {
            return
        }

        if (isFarOffScreen(
                topLeftPixel,
                topRightPixel,
                bottomLeftPixel,
                bottomRightPixel,
                drawer.width,
                drawer.height
            )
        ) {
            return
        }

        val actualWidth = bitmap.width - 2 * borderPixels
        val actualHeight = bitmap.height - 2 * borderPixels

        // Bitmap pixels, exclude the border pixels - this is the bitmap with the borders cropped off (since we only render a region of the bitmap)
        // Top left
        srcPoints[0] = 0f
        srcPoints[1] = 0f
        // Top right
        srcPoints[2] = actualWidth.toFloat()
        srcPoints[3] = 0f
        // Bottom left
        srcPoints[4] = 0f
        srcPoints[5] = actualHeight.toFloat()
        // Bottom right
        srcPoints[6] = actualWidth.toFloat()
        srcPoints[7] = actualHeight.toFloat()

        // Canvas pixels
        // Top left
        dstPoints[0] = topLeftPixel.x
        dstPoints[1] = topLeftPixel.y
        // Top right
        dstPoints[2] = topRightPixel.x
        dstPoints[3] = topRightPixel.y
        // Bottom left
        dstPoints[4] = bottomLeftPixel.x
        dstPoints[5] = bottomLeftPixel.y
        // Bottom right
        dstPoints[6] = bottomRightPixel.x
        dstPoints[7] = bottomRightPixel.y

        if (!renderMatrix.setPolyToPoly(srcPoints, dstPoints)) {
            return
        }

        // Fade the tile in
        val alpha = imageTile.getAlpha()
        // There are still tiles being faded in, so keep re-rendering the map
        if (alpha != 255) {
            hasFadingTiles = true
        }
        drawer.opacity(alpha)

        drawer.withTransform(renderMatrix) {
            // Only the region of the bitmap inside its border, drawn at its own size
            drawer.image(
                bitmap,
                borderPixels,
                borderPixels,
                actualWidth,
                actualHeight,
                0f,
                0f,
                actualWidth.toFloat(),
                actualHeight.toFloat()
            )
        }
    }

    private fun renderTileClipped(
        imageTile: SharedImageTile<*>,
        drawer: MapDrawer,
        projection: IMapViewProjection,
        bitmap: ImageBitmap,
        clipTile: Tile
    ) {
        val corners = getProjectedCorners(clipTile, projection)
        val clipTopLeft = corners[0]
        val clipTopRight = corners[1]
        val clipBottomRight = corners[2]
        val clipBottomLeft = corners[3]

        drawer.push()
        try {
            clipPath.rewind()
            clipPath.moveTo(clipTopLeft.x, clipTopLeft.y)
            clipPath.lineTo(clipTopRight.x, clipTopRight.y)
            clipPath.lineTo(clipBottomRight.x, clipBottomRight.y)
            clipPath.lineTo(clipBottomLeft.x, clipBottomLeft.y)
            clipPath.close()
            drawer.clip(clipPath)
            renderTile(imageTile, drawer, projection, bitmap)
        } finally {
            drawer.pop()
        }
    }

    private fun getProjectedCorners(
        tile: Tile,
        projection: IMapViewProjection
    ): Array<PixelCoordinate> = projectedTiles.getOrPut(tile) {
        val bounds = tile.getBounds()
        arrayOf(
            projection.toPixels(bounds.northWest),
            projection.toPixels(bounds.northEast),
            projection.toPixels(bounds.southEast),
            projection.toPixels(bounds.southWest)
        )
    }

}
