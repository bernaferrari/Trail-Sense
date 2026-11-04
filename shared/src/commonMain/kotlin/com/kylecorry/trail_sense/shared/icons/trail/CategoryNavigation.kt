package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_category_navigation.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.CategoryNavigation: ImageVector
    get() {
        _CategoryNavigation?.let { return it }
        return ImageVector.Builder(
            name = "CategoryNavigation",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M14.19 14.19L6 18l3.81-8.19L18 6m-6-4A10 10 0 0 0 2 12a10 10 0 0 0 10 10 10 10 0 0 0 10-10A10 10 0 0 0 12 2m0 8.9a1.1 1.1 0 0 0-1.1 1.1 1.1 1.1 0 0 0 1.1 1.1 1.1 1.1 0 0 0 1.1-1.1 1.1 1.1 0 0 0-1.1-1.1z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _CategoryNavigation = it }
    }

private var _CategoryNavigation: ImageVector? = null
