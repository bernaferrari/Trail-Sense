package com.kylecorry.trail_sense.tools.map.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * A folded map with a "you are here" dot in its middle panel: the Map tab's empty state, so it reads as "where am I
 * on my map" rather than the same map glyph as the Offline Maps list.
 */
internal val MapHereIcon: ImageVector
    get() {
        _mapHereIcon?.let { return it }
        return ImageVector.Builder(
            name = "MapHere",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // The folded map (Material Symbols Rounded "map"); its middle panel is open.
            path(fill = SolidColor(Color.Black)) {
                moveTo(14.35f, 20.78f)
                lineTo(9f, 18.9f)
                lineTo(4.35f, 20.7f)
                quadTo(4.1f, 20.8f, 3.86f, 20.76f)
                reflectiveQuadTo(3.43f, 20.6f)
                reflectiveQuadTo(3.11f, 20.26f)
                reflectiveQuadTo(3f, 19.77f)
                verticalLineTo(5.75f)
                quadTo(3f, 5.43f, 3.19f, 5.18f)
                reflectiveQuadTo(3.7f, 4.8f)
                lineTo(8.35f, 3.22f)
                quadTo(8.5f, 3.17f, 8.66f, 3.15f)
                quadTo(8.83f, 3.13f, 9f, 3.13f)
                quadToRelative(0.18f, 0f, 0.34f, 0.02f)
                reflectiveQuadTo(9.65f, 3.22f)
                lineTo(15f, 5.1f)
                lineTo(19.65f, 3.3f)
                quadTo(19.9f, 3.2f, 20.14f, 3.24f)
                reflectiveQuadTo(20.58f, 3.4f)
                reflectiveQuadToRelative(0.31f, 0.34f)
                reflectiveQuadTo(21f, 4.22f)
                verticalLineTo(18.25f)
                quadToRelative(0f, 0.32f, -0.19f, 0.57f)
                reflectiveQuadTo(20.3f, 19.2f)
                lineToRelative(-4.65f, 1.57f)
                quadToRelative(-0.15f, 0.05f, -0.31f, 0.08f)
                reflectiveQuadTo(15f, 20.88f)
                reflectiveQuadTo(14.66f, 20.85f)
                reflectiveQuadTo(14.35f, 20.78f)
                close()
                moveTo(14f, 18.55f)
                verticalLineTo(6.85f)
                lineTo(10f, 5.45f)
                verticalLineToRelative(11.7f)
                lineToRelative(4f, 1.4f)
                close()
            }
            // You are here: a dot in the open panel.
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 10.4f)
                arcToRelative(1.6f, 1.6f, 0f, true, true, 0f, 3.2f)
                arcToRelative(1.6f, 1.6f, 0f, true, true, 0f, -3.2f)
                close()
            }
        }.build().also { _mapHereIcon = it }
    }

private var _mapHereIcon: ImageVector? = null
