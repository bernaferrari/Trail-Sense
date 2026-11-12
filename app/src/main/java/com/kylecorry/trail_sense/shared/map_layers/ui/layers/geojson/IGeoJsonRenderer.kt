package com.kylecorry.trail_sense.shared.map_layers.ui.layers.geojson

import com.kylecorry.trail_sense.shared.canvas.MapDrawer
import com.kylecorry.andromeda.core.units.PixelCoordinate
import com.kylecorry.trail_sense.shared.map_layers.MapViewState
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapView

interface IGeoJsonRenderer {
    fun setHasUpdateListener(listener: (() -> Unit)?)
    fun draw(drawer: MapDrawer, map: IMapView)
    fun onViewChanged(state: MapViewState)
    fun invalidate()
    fun onClick(drawer: MapDrawer, map: IMapView, pixel: PixelCoordinate): Boolean
}
