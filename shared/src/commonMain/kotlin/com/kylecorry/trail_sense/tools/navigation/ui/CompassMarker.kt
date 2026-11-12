package com.kylecorry.trail_sense.tools.navigation.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.graphics.shapes.RoundedPolygon

/** An icon drawn on a compass at a bearing, such as a beacon, the sun or the destination. */
data class CompassMarker(
    val bearing: Float,
    val icon: ImageVector,
    val tint: Color? = null,
    val opacity: Float = 1f,
    val rotation: Float = 0f,
    /** The icon's size, or null for the compass's standard icon size. */
    val size: Dp? = null,
    /** The outline a waypoint wears on the radar's rim (a beacon's chosen shape), or null for the standard mark. */
    val shape: RoundedPolygon? = null
)

/** A direction highlighted on a compass, such as the bearing to a destination. */
data class CompassBearingIndicator(
    val bearing: Float,
    val color: Color
)
