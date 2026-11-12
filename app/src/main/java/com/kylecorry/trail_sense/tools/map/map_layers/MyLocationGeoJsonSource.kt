package com.kylecorry.trail_sense.tools.map.map_layers

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerValues
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

import android.content.Context
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import com.kylecorry.trail_sense.shared.canvas.ComposeMapDrawer
import com.kylecorry.trail_sense.shared.colors.ColorInts
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.andromeda.geojson.GeoJsonObject
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.main.getAppService
import com.kylecorry.trail_sense.shared.CustomUiUtils.getPrimaryMarkerColor
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.sources.GeoJsonSource
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerParams
import com.kylecorry.trail_sense.shared.colors.AppColor
import com.kylecorry.trail_sense.shared.sensors.LocationSubsystem
import com.kylecorry.trail_sense.shared.sensors.SensorService

class MyLocationGeoJsonSource : GeoJsonSource {

    private val context: Context
        get() = getAppService()

    private var arrowBitmap: ImageBitmap? = null
    private var arrowBitmapKey: ArrowBitmapKey? = null
    private val hasCompass = DependencyRegistry.get<SensorService>().hasCompass()

    override suspend fun load(
        bounds: CoordinateBounds,
        zoom: Int,
        params: MapLayerValues
    ): GeoJsonObject {
        val preferences = params.getPreferences()
        val drawAccuracy = preferences.getBoolean(
            SHOW_ACCURACY,
            DEFAULT_SHOW_ACCURACY
        )
        val largeIndicator = preferences.getBoolean(
            LARGE_INDICATOR,
            DEFAULT_LARGE_INDICATOR
        )
        val userLocationSizeDp = if (largeIndicator) LARGE_SIZE_DP else DEFAULT_SIZE_DP
        val strokeWeight = if (largeIndicator) 4f else 2f
        val isStale = getAppService<LocationSubsystem>().isLocationStale
        val color = if (isStale) {
            AppColor.Gray.color
        } else {
            Resources.getPrimaryMarkerColor(context)
        }
        val features = mutableListOf<GeoJsonFeature>()

        if (drawAccuracy) {
            features.add(
                GeoJsonFeature.point(
                    Coordinate.zero,
                    color = color,
                    opacity = 25,
                    moveWithUserLocation = true,
                    scaleToLocationAccuracy = true
                )
            )
        }

        val isWidget = params.getBoolean(MapLayerParams.PARAM_IS_WIDGET, false)

        if (hasCompass && !isWidget) {
            features.add(
                GeoJsonFeature.point(
                    Coordinate.zero,
                    bitmap = getArrowBitmap(context, color, userLocationSizeDp, strokeWeight),
                    size = userLocationSizeDp,
                    moveWithUserLocation = true,
                    rotateWithUserAzimuth = true
                )
            )
        } else {
            features.add(
                GeoJsonFeature.point(
                    Coordinate.zero,
                    color = color,
                    strokeColor = ColorInts.White,
                    strokeWeight = strokeWeight,
                    size = userLocationSizeDp,
                    moveWithUserLocation = true
                )
            )
        }
        return GeoJsonFeatureCollection(features)
    }

    private fun getArrowBitmap(context: Context, color: Int, sizeDp: Float, strokeWeight: Float): ImageBitmap {
        val key = ArrowBitmapKey(color, sizeDp, strokeWeight)
        val cached = arrowBitmap
        if (cached != null && arrowBitmapKey == key) {
            return cached
        }

        val size = Resources.dp(context, sizeDp).toInt()
        val bitmap = ImageBitmap(size, size)
        val bitmapDrawer = ComposeMapDrawer(
            context.resources.displayMetrics.density,
            context.resources.configuration.fontScale
        )
        bitmapDrawer.setCanvas(Canvas(bitmap), size.toFloat(), size.toFloat())

        val path = Path()
        // Bottom left
        path.moveTo(size * 0.1f, size.toFloat())

        // Top
        path.lineTo(size / 2f, 0f)

        // Bottom right
        path.lineTo(size * 0.9f, size.toFloat())

        // Middle dip
        path.lineTo(size / 2f, size * 0.8f)

        path.close()
        bitmapDrawer.push()
        bitmapDrawer.fill(color)
        bitmapDrawer.stroke(ColorInts.White)
        bitmapDrawer.strokeWeight(strokeWeight)
        bitmapDrawer.path(path)
        bitmapDrawer.pop()

        arrowBitmap = bitmap
        arrowBitmapKey = key
        return bitmap
    }

    override suspend fun cleanup() {
        arrowBitmap = null
    }

    private data class ArrowBitmapKey(val color: Int, val sizeDp: Float, val strokeWeight: Float)

    companion object {
        const val SOURCE_ID = "my_location"
        const val SHOW_ACCURACY = "show_accuracy"
        const val DEFAULT_SHOW_ACCURACY = true
        const val LARGE_INDICATOR = "large_indicator"
        const val DEFAULT_LARGE_INDICATOR = false
        private const val DEFAULT_SIZE_DP = 20f
        private const val LARGE_SIZE_DP = 28f
        val STALE_CHECK_INTERVAL: Duration = 1.minutes
    }

}
