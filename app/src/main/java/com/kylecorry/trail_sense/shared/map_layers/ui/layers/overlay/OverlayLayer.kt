package com.kylecorry.trail_sense.shared.map_layers.ui.layers.overlay

import com.kylecorry.trail_sense.shared.map_layers.ui.layers.MapLayerValues
import com.kylecorry.trail_sense.shared.canvas.MapDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.ILayer
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView

abstract class OverlayLayer : ILayer {

    override fun setPreferences(preferences: MapLayerValues) {
        // Do nothing
    }

    override fun draw(
        drawer: MapDrawer,
        map: IMapView
    ) {
        // Do nothing
    }

    override fun invalidate() {
        // Do nothing
    }

    override fun onClick(
        drawer: MapDrawer,
        map: IMapView,
        pixel: PixelCoordinate
    ): Boolean {
        return false
    }

    override var percentOpacity: Float = 1f
}
