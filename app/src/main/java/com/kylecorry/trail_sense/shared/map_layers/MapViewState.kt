package com.kylecorry.trail_sense.shared.map_layers

import com.kylecorry.sol.math.geometry.Rectangle
import com.kylecorry.sol.science.geology.CoordinateBounds
import com.kylecorry.trail_sense.shared.map_layers.ui.layers.IMapViewProjection

class MapViewState(
    val viewBounds: Rectangle,
    val bounds: CoordinateBounds,
    val projection: IMapViewProjection
)
