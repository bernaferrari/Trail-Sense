package com.kylecorry.trail_sense.shared.dem.map_layers

import com.kylecorry.trail_sense.shared.dem.DEMZoomLevels

import android.graphics.Bitmap
import com.kylecorry.andromeda.bitmaps.operations.Conditional
import com.kylecorry.andromeda.bitmaps.operations.Dither
import com.kylecorry.andromeda.bitmaps.operations.ReplaceColor
import com.kylecorry.andromeda.bitmaps.operations.applyOperations
import com.kylecorry.sol.math.MathExtensions.toDegrees
import com.kylecorry.trail_sense.shared.colors.ColorInts
import com.kylecorry.trail_sense.shared.dem.DEM
import com.kylecorry.trail_sense.shared.dem.colors.SlopeColorMapFactory
import com.kylecorry.trail_sense.shared.dem.colors.SlopeColorStrategy
import com.kylecorry.trail_sense.shared.dem.getCellSizeX
import com.kylecorry.trail_sense.shared.dem.getCellSizeY
import com.kylecorry.trail_sense.shared.dem.getSlopeAngle
import com.kylecorry.trail_sense.shared.dem.getSlopeVector
import com.kylecorry.trail_sense.shared.map_layers.tiles.Tile
import com.kylecorry.trail_sense.shared.map_layers.tiles.toSize
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerValues
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.tiles.TileSource
import com.kylecorry.trail_sense.shared.withId
import kotlin.math.absoluteValue

class SlopeMapTileSource : TileSource<Bitmap> {

    override suspend fun loadTile(tile: Tile, params: MapLayerValues): Bitmap? {
        val preferences = params.getPreferences()
        val strategyId = preferences.getString(COLOR)?.toLongOrNull()
        val colorMap = SlopeColorMapFactory().getSlopeColorMap(
            SlopeColorStrategy.entries.withId(strategyId ?: 0) ?: DEFAULT_COLOR
        )
        val highResolution =
            preferences.getBoolean(
                HIGH_RESOLUTION,
                DEFAULT_HIGH_RESOLUTION
            )
        val smooth = preferences.getBoolean(SMOOTH, DEFAULT_SMOOTH)
        val hideFlatGround =
            preferences.getBoolean(
                HIDE_FLAT_GROUND,
                DEFAULT_HIDE_FLAT_GROUND
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

        val flatColor = colorMap.getSlopeColor(0f)

        val padding = 2
        return DEM.getElevationImage(
            bounds,
            resolution,
            tile.size.toSize(),
            config = Bitmap.Config.ARGB_8888,
            padding = padding,
            oceanColor = flatColor,
            inlandWaterColor = flatColor
        ) { x, y, getElevation ->
            val vector = getSlopeVector(cellSizeX, cellSizeY, x, y, getElevation)
            val slopeDegrees = getSlopeAngle(vector).toDegrees().absoluteValue

            if (hideFlatGround && slopeDegrees <= 10f) {
                return@getElevationImage flatColor
            }

            val actualDegrees = if (smooth) {
                slopeDegrees
            } else {
                when {
                    slopeDegrees <= 10f -> 0f
                    slopeDegrees <= 25f -> 10f
                    else -> 90f
                }
            }

            colorMap.getSlopeColor(actualDegrees)
        }.applyOperations(
            Conditional(
                !hideFlatGround,
                Dither(Bitmap.Config.RGB_565)
            ),
            Conditional(
                hideFlatGround,
                ReplaceColor(
                    flatColor,
                    ColorInts.Transparent,
                    80f,
                    interpolate = false,
                    inPlace = true
                )
            )
        )
    }

    companion object {
        const val SOURCE_ID = "slope"
        const val COLOR = "color"
        const val HIGH_RESOLUTION = "high_resolution"
        const val SMOOTH = "smooth"
        const val HIDE_FLAT_GROUND = "hide_flat_ground"
        val DEFAULT_COLOR = SlopeColorStrategy.GreenToRed
        const val DEFAULT_HIGH_RESOLUTION = false
        const val DEFAULT_SMOOTH = true
        const val DEFAULT_HIDE_FLAT_GROUND = false
    }

}
