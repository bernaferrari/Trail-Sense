package com.kylecorry.trail_sense.tools.navigation.ui.markers

import com.kylecorry.trail_sense.shared.canvas.MapDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.sol.units.Coordinate

interface MapMarker {
    val location: Coordinate?
    val size: Float
    val rotation: Float?
    val rotateWithUser: Boolean
    val scaleToLocationAccuracy: Boolean
    fun draw(
        drawer: MapDrawer,
        anchor: PixelCoordinate,
        scale: Float,
        rotation: Float,
        metersPerPixel: Float
    )

    fun onClick(): Boolean
    fun calculateSizeInPixels(drawer: MapDrawer, metersPerPixel: Float, scale: Float): Float
}
