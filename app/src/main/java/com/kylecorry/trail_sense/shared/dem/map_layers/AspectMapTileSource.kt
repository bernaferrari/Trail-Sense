package com.kylecorry.trail_sense.shared.dem.map_layers

import com.kylecorry.trail_sense.shared.dem.DEMZoomLevels

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerValues
import com.kylecorry.trail_sense.shared.map_layers.tiles.toSize

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.operations.Dither
import com.kylecorry.andromeda.bitmaps.operations.applyOperations
import com.kylecorry.andromeda.core.ui.Colors
import com.kylecorry.andromeda.core.ui.colormaps.RgbInterpolationColorMap
import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.sol.math.trigonometry.Trigonometry
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.getCellSizeX
import com.kylecorry.trail_sense.shared.dem.getCellSizeY
import com.kylecorry.trail_sense.shared.dem.getSlopeAngle
import com.kylecorry.trail_sense.shared.dem.getSlopeAspect
import com.kylecorry.trail_sense.shared.dem.getSlopeVector
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import kotlin.math.absoluteValue

class AspectMapTileSource : TileSource<Bitmap> {
    private val colorMap = RgbInterpolationColorMap(
        arrayOf(
            AppColor.Green.color,
            Colors.interpolate(AppColor.Green.color, AppColor.Blue.color, 0.5f),
            AppColor.Blue.color,
            AppColor.Purple.color,
            AppColor.Red.color,
            Colors.interpolate(AppColor.Red.color, AppColor.Orange.color, 0.5f),
            AppColor.Orange.color,
            AppColor.Yellow.color,
            AppColor.Green.color
        )
    )

    override suspend fun loadTile(tile: Tile, params: MapLayerValues): Bitmap? {
        val preferences = params.getPreferences()
        val highResolution =
            preferences.getBoolean(
                HIGH_RESOLUTION,
                DEFAULT_HIGH_RESOLUTION
            )

        val zoomLevel = tile.z.coerceIn(DEMZoomLevels.IMAGE_MIN_ZOOM_LEVEL, DEMZoomLevels.IMAGE_MAX_ZOOM_LEVEL)
        val bounds = tile.getBounds()

        val zoomToResolutionMap = if (highResolution) {
            DEMZoomLevels.HIGH_RESOLUTION_ZOOM_TO_RESOLUTION
        } else {
            DEMZoomLevels.LOW_RESOLUTION_ZOOM_TO_RESOLUTION
        }
        val resolution = zoomToResolutionMap[zoomLevel] ?: return null

        val cellSizeX = getCellSizeX(resolution, bounds)
        val cellSizeY = getCellSizeY(resolution)

        val padding = 2
        return DEM.getElevationImage(
            bounds,
            resolution,
            tile.size.toSize(),
            config = Bitmap.Config.ARGB_8888,
            padding = padding,
            oceanColor = AppColor.Gray.color,
            inlandWaterColor = AppColor.Gray.color
        ) { x, y, getElevation ->
            val vector = getSlopeVector(cellSizeX, cellSizeY, x, y, getElevation)
            val slopeDegrees = getSlopeAngle(vector).toDegrees().absoluteValue

            if (slopeDegrees < 1f) {
                return@getElevationImage AppColor.Gray.color
            }

            val aspect = Trigonometry.remapAngle(
                getSlopeAspect(vector).toDegrees(),
                180f,
                false,
                90f,
                false
            )
            colorMap.getColor(aspect / 360f)
        }.applyOperations(
            Dither(Bitmap.Config.RGB_565)
        )
    }

    companion object {
        const val SOURCE_ID = "aspect"
        const val HIGH_RESOLUTION = "high_resolution"
        const val DEFAULT_HIGH_RESOLUTION = false
    }

}
