package com.kylecorry.trail_sense.shared.map_layers.ui.layers

import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.andromeda.geojson.GeoJsonFeature
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.sol.units.Bearing
import com.kylecorry.sol.units.Coordinate
import com.kylecorry.sol.units.Distance
import com.kylecorry.trail_sense.shared.map_layers.MapViewLayerManager

interface IMapView {

    val layerManager: MapViewLayerManager

    /**
     * The current projection of the map. The response should be fixed, so it doesn't change on consumers using it.
     */
    val mapProjection: IMapViewProjection

    /**
     * Whether this map view is being rendered as a widget (e.g. home screen widget).
     */
    var isWidget: Boolean

    /**
     * Whether layers should render at physical pixel resolution instead of density-adjusted resolution.
     */
    val isHighDetailMode: Boolean

    var userLocation: Coordinate

    var userLocationAccuracy: Distance?

    var userAzimuth: Bearing

    /**
     * The resolution in meters per pixel
     */
    var resolutionPixels: Float

    /**
     * Zoom level using resolution
     */
    val zoom: Float

    /**
     * The resolution in meters per density pixel
     */
    var resolution: Float

    /**
     * The scale for each layer element
     * This is likely to be temporary until meters per pixel can be used
     */
    val layerScale: Float

    /**
     * The center of the map
     */
    var mapCenter: Coordinate

    /**
     * The azimuth of the MapView
     */
    var mapAzimuth: Float

    /**
     * The rotation of the map from azimuth. Ex. the top of the map doesn't point north
     */
    val mapRotation: Float

    val mapBounds: CoordinateBounds

    /**
     * Set the feature click listener for the map
     */
    fun setOnGeoJsonFeatureClickListener(listener: ((GeoJsonFeature) -> Unit)?)
}

fun IMapView.toPixel(coordinate: Coordinate): PixelCoordinate {
    return mapProjection.toPixels(coordinate)
}

fun IMapView.toPixel(latitude: Double, longitude: Double): PixelCoordinate {
    return mapProjection.toPixels(latitude, longitude)
}

fun IMapView.toCoordinate(pixel: PixelCoordinate): Coordinate {
    return mapProjection.toCoordinate(pixel)
}

fun IMapView.setLayers(layers: List<ILayer>) {
    layerManager.setLayers(layers)
}

fun IMapView.start() {
    layerManager.start()
}

fun IMapView.stop() {
    layerManager.stop()
}

fun IMapView.getLayerById(layerId: String): ILayer? {
    return layerManager.getLayers().firstOrNull { it.layerId == layerId }
}
