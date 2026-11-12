package com.kylecorry.trail_sense.tools.navigation.ui

import androidx.compose.ui.graphics.vector.ImageVector
import com.kylecorry.trail_sense.shared.data.Identifiable

interface IMappableReferencePoint : Identifiable {
    val icon: ImageVector
    val tint: Int?
    val opacity: Float
    val bearing: Float
    val rotation: Float

    /** The outline a waypoint wears on the rim (a beacon's chosen shape), or null for a plain dot. */
    val shape: androidx.graphics.shapes.RoundedPolygon?
        get() = null
}

data class MappableReferencePoint(
    override val id: Long,
    override val icon: ImageVector,
    override val bearing: Float,
    override val tint: Int? = null,
    override val opacity: Float = 1f,
    override val rotation: Float = 0f,
    override val shape: androidx.graphics.shapes.RoundedPolygon? = null
) : IMappableReferencePoint
