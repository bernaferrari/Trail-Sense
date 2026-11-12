package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson.features

import androidx.compose.ui.graphics.Matrix
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection
import com.kylecorry.trail_sense.shared.canvas.scaleX
import com.kylecorry.trail_sense.shared.canvas.setPolyToPoly

internal class GeoJsonRenderTransform {
    val matrix = Matrix()
    var scale = 1f
        private set
    private val destination = FloatArray(8)
    private var lastProjection: IMapViewProjection? = null
    private var lastBounds: CoordinateBounds? = null
    private var lastCorners: FloatArray? = null

    fun update(projection: IMapViewProjection, bounds: CoordinateBounds, corners: FloatArray) {
        if (projection === lastProjection && bounds == lastBounds && corners === lastCorners) {
            return
        }
        val nw = projection.toPixels(bounds.northWest)
        val ne = projection.toPixels(bounds.northEast)
        val se = projection.toPixels(bounds.southEast)
        val sw = projection.toPixels(bounds.southWest)
        destination[0] = nw.x
        destination[1] = nw.y
        destination[2] = ne.x
        destination[3] = ne.y
        destination[4] = se.x
        destination[5] = se.y
        destination[6] = sw.x
        destination[7] = sw.y
        matrix.setPolyToPoly(corners, destination)
        scale = matrix.scaleX
        lastProjection = projection
        lastBounds = bounds
        lastCorners = corners
    }
}
