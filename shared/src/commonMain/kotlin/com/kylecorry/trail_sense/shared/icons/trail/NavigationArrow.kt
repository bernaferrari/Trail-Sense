package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_navigation_arrow.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.NavigationArrow: ImageVector
    get() {
        _NavigationArrow?.let { return it }
        return ImageVector.Builder(
            name = "NavigationArrow",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M12,2.5L21.5,12H15.5V21.5H8.5V12H2.5Z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _NavigationArrow = it }
    }

private var _NavigationArrow: ImageVector? = null
