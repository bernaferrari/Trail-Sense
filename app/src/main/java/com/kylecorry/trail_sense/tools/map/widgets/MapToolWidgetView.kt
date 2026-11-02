package com.kylecorry.trail_sense.tools.map.widgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.view.View
import androidx.core.graphics.createBitmap
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.luna.concurrency.onMain
import com.kylecorry.sol.science.geography.projections.AzimuthalEquidistantProjection
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.navigation.NavigationUtils
import com.kylecorry.trail_sense.shared.sensors.SensorSubsystem
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.map.ui.MapView
import com.kylecorry.trail_sense.tools.navigation.map_layers.CompassOverlayLayer
import com.kylecorry.trail_sense.tools.tools.infrastructure.Tools
import com.kylecorry.trail_sense.tools.tools.ui.widgets.ToolWidgetContent
import com.kylecorry.trail_sense.tools.tools.ui.widgets.WidgetClickAction
import com.kylecorry.trail_sense.tools.tools.widgets.ChartToolWidgetViewBase
import com.kylecorry.trail_sense.tools.tools.widgets.WidgetPreferences
import kotlinx.coroutines.delay

class MapToolWidgetView : ChartToolWidgetViewBase() {

    private var drawAsCircle: Boolean = false

    override suspend fun getContent(
        context: Context,
        prefs: WidgetPreferences?
    ): ToolWidgetContent {
        val repo = DependencyRegistry.get<MapLayerPreferenceRepo>()
        var renderedMap: android.graphics.Bitmap? = null

        onMain {
            renderedMap = renderMap(context, repo)
        }

        return ToolWidgetContent.Chart(
            bitmap = renderedMap,
            clickAction = WidgetClickAction.StartActivity(NavigationUtils.toolIntent(context, Tools.MAP))
        )
    }

    private suspend fun renderMap(context: Context, repo: MapLayerPreferenceRepo): Bitmap {
        val location = SensorSubsystem.getInstance(context).lastKnownLocation
        val size = Resources.dp(context, 400f).toInt()
        val mapView = createMapView(context, size, location)
        val layerIds = repo.getActiveLayerIds(MapToolRegistration.MAP_ID) + listOf(
            ScaleBarLayer.LAYER_ID,
            CompassOverlayLayer.LAYER_ID
        )
        mapView.setLayersWithPreferences(MapToolRegistration.MAP_ID, layerIds)

        mapView.measure(
            View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
        )
        mapView.layout(0, 0, size, size)
        return drawMap(mapView, size)
    }

    private fun createMapView(
        context: Context,
        size: Int,
        location: Coordinate
    ): MapView {
        val cornerRadius = Resources.dp(context, 8f)
        val mapView = MapView(context)
        mapView.backgroundColorOverride = Color.rgb(127, 127, 127)
        mapView.clipPath = Path().apply {
            if (drawAsCircle) {
                addCircle(size / 2f, size / 2f, size / 2f, Path.Direction.CW)
            } else {
                addRoundRect(
                    0f,
                    0f,
                    size.toFloat(),
                    size.toFloat(),
                    FloatArray(8) { cornerRadius },
                    Path.Direction.CW
                )
            }
        }
        mapView.isInteractive = false
        mapView.isWidget = true
        mapView.mapCenter = location
        mapView.mapAzimuth = 0f
        mapView.userLocation = location
        mapView.resolution = 2f
        if (drawAsCircle) {
            mapView.projection = AzimuthalEquidistantProjection(location)
            mapView.metersPerProjectedUnit = 1.0
            mapView.latitudeScaleFactor = { 1f }
        }
        return mapView
    }

    private suspend fun drawMap(mapView: MapView, size: Int): Bitmap {
        mapView.layerManager.start()
        try {
            val bitmap = createBitmap(size, size)
            val canvas = Canvas(bitmap)
            var totalDelay = 0
            val maxDelay = 2000
            while (totalDelay < maxDelay && mapView.layerManager.getLayers().any { !it.isLoaded }) {
                mapView.draw(canvas)
                delay(100)
                totalDelay += 100
            }
            repeat(3) {
                mapView.draw(canvas)
            }
            return bitmap
        } finally {
            mapView.layerManager.stop()
        }
    }
}
