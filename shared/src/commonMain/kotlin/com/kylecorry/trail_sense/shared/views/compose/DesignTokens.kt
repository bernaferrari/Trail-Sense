package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens for Trail Sense — Material 3 Expressive foundation.
 */
object TrailSenseSpacing {
    val xxs = 2.dp
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 12.dp
    val lg  = 16.dp
    val screen = 20.dp
    val xl  = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/** Sizing tokens shared by every destination so controls remain comfortable outdoors. */
object TrailSenseSize {
    val minimumTouchTarget = 48.dp
    val leadingIconContainer = 48.dp
    val leadingIcon = 24.dp
    val compactIconContainer = 40.dp
    val compactIcon = 20.dp
    val maximumContentWidth = 720.dp
}

/**
 * Expressive grouped-list geometry, matching QuietGuard: generous outer corners and a small
 * inter-item radius. The changing silhouette makes a section read as one object without dividers.
 */
object TrailSenseShape {
    val groupOuter = 24.dp
    val groupInner = 6.dp
    val iconContainer = 16.dp
    val hero = 32.dp
}

@Immutable
data class TrailSenseGroupPosition(
    val isFirst: Boolean,
    val isLast: Boolean
) {
    companion object {
        val Single = TrailSenseGroupPosition(isFirst = true, isLast = true)

        fun at(index: Int, count: Int): TrailSenseGroupPosition = TrailSenseGroupPosition(
            isFirst = index == 0,
            isLast = index == count - 1
        )
    }
}

fun trailSenseGroupShape(
    position: TrailSenseGroupPosition
): RoundedCornerShape = RoundedCornerShape(
    topStart = if (position.isFirst) TrailSenseShape.groupOuter else TrailSenseShape.groupInner,
    topEnd = if (position.isFirst) TrailSenseShape.groupOuter else TrailSenseShape.groupInner,
    bottomEnd = if (position.isLast) TrailSenseShape.groupOuter else TrailSenseShape.groupInner,
    bottomStart = if (position.isLast) TrailSenseShape.groupOuter else TrailSenseShape.groupInner
)

object TrailSenseElevation {
    val none    = 0.dp
    val low     = 1.dp
    val medium  = 3.dp
    val high    = 6.dp
    val highest = 12.dp
}

object TrailSenseMotion {
    const val short  = 150
    const val medium = 250
    const val long   = 400
}
