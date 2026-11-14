package com.kylecorry.trail_sense.tools.map.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.andromeda.geojson.GeoJsonFeatureCollection
import com.kylecorry.sol.science.geography.Geography
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.colors.ColorInts
import com.kylecorry.trail_sense.shared.extensions.inBackground
import com.kylecorry.trail_sense.shared.extensions.point
import com.kylecorry.trail_sense.shared.map_layers.preferences.repo.MapLayerPreferenceRepo
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.ConfigurableGeoJsonLayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.getLayerById
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.setLayersWithPreferences
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.start
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.stop
import com.kylecorry.trail_sense.tools.map.MapToolRegistration
import com.kylecorry.trail_sense.tools.map.map_layers.MyElevationLayer
import com.kylecorry.trail_sense.tools.map.map_layers.ScaleBarLayer
import com.kylecorry.trail_sense.tools.navigation.map_layers.CompassOverlayLayer
import com.kylecorry.trail_sense.tools.offline_maps.ui.photo_maps.MapDistanceLayer
import com.kylecorry.trail_sense.tools.paths.domain.PathEditState
import com.kylecorry.trail_sense.tools.paths.infrastructure.PathElevationPrefetcher
import kotlin.time.Instant

class MapToolLayerManager(useFastPathRendering: Boolean) {
    private val selectedPointLayer = ConfigurableGeoJsonLayer(useFastPathRendering)
    private val distanceLayer = MapDistanceLayer().apply { isEnabled = false }
    private var onDistanceChangedCallback: ((Distance) -> Unit)? = null
    private var onDistanceEditStateChanged: ((PathEditState) -> Unit)? = null
    private val elevationPrefetcher = PathElevationPrefetcher()
    private val repo = DependencyRegistry.get<MapLayerPreferenceRepo>()

    /** Changes each time the layers are rebuilt; observable from Compose. */
    var key by mutableIntStateOf(0)
        private set

    private var isRunning = false
    private val runningLock = Any()

    fun resume(
        view: IMapView,
        lifecycleOwner: LifecycleOwner,
        onFeatureClick: (GeoJsonFeature) -> Unit
    ) {
        synchronized(runningLock) {
            isRunning = true
        }
        lifecycleOwner.inBackground {
            view.setLayersWithPreferences(
                MapToolRegistration.MAP_ID,
                repo.getActiveLayerIds(MapToolRegistration.MAP_ID) +
                        listOf(
                            ScaleBarLayer.LAYER_ID,
                            MyElevationLayer.LAYER_ID,
                            CompassOverlayLayer.LAYER_ID,
                        ),
                listOf(
                    selectedPointLayer,
                    distanceLayer
                )
            )

            // Hardcoded configuration
            distanceLayer.onPathChanged = { onDistancePathChange(it) }
            distanceLayer.onPathDragged = { updateDistance(it) }
            distanceLayer.onEditStateChanged = { onDistanceEditStateChanged?.invoke(it) }
            (view.getLayerById(CompassOverlayLayer.LAYER_ID) as? CompassOverlayLayer)?.paddingTopDp =
                48f

            view.layerManager.setOnGeoJsonFeatureClickListener { feature ->
                onFeatureClick(feature)
            }

            synchronized(runningLock) {
                if (isRunning) {
                    view.start()
                }
            }

            key++
        }
    }

    fun pause(view: IMapView) {
        synchronized(runningLock) {
            isRunning = false
            view.stop()
            view.layerManager.setOnGeoJsonFeatureClickListener(null)
        }
    }

    fun onBoundsChanged() {
        distanceLayer.invalidate()
    }

    fun setTime(view: IMapView, time: Instant?) {
        view.layerManager.setTime(time)
    }

    fun setSelectedLocation(location: Coordinate?) {
        if (location == null) {
            selectedPointLayer.setData(GeoJsonFeatureCollection(emptyList()))
        } else {
            val point = GeoJsonFeature.point(
                location,
                strokeColor = ColorInts.White,
                color = ColorInts.Black
            )
            selectedPointLayer.setData(point)
        }
    }

    // Distance measurement

    private fun onDistancePathChange(points: List<Coordinate>) {
        elevationPrefetcher.update(points)
        updateDistance(points)
    }

    private fun updateDistance(points: List<Coordinate>) {
        val distance = Geography.getPathDistance(points)
        onDistanceChangedCallback?.invoke(distance)
    }

    fun setOnDistanceChangedCallback(callback: ((Distance) -> Unit)?) {
        onDistanceChangedCallback = callback
    }

    fun stopDistanceMeasurement() {
        distanceLayer.isEnabled = false
        distanceLayer.clear()
    }

    fun undoLastDistanceMeasurement() {
        distanceLayer.undo()
    }

    fun redoDistanceMeasurement() {
        distanceLayer.redo()
    }

    fun getDistanceEditState(): PathEditState {
        return distanceLayer.editState
    }

    fun setOnDistanceEditStateChangedCallback(callback: ((PathEditState) -> Unit)?) {
        onDistanceEditStateChanged = callback
    }

    fun getDistanceMeasurementPoints(): List<Coordinate> {
        return distanceLayer.getPoints()
    }

    fun getDistanceMeasurementElevations(): Map<Coordinate, Float> {
        return elevationPrefetcher.getElevations()
    }

    fun startDistanceMeasurement(initialPoints: List<Coordinate>) {
        distanceLayer.isEnabled = true
        distanceLayer.clear()
        initialPoints.forEach { distanceLayer.add(it) }
    }

    fun isMeasuringDistance(): Boolean {
        return distanceLayer.isEnabled
    }
}
