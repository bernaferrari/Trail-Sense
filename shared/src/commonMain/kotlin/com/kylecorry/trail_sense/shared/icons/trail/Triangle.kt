package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/triangle.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Triangle: ImageVector
    get() {
        _Triangle?.let { return it }
        return ImageVector.Builder(
            name = "Triangle",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M1,21H23L12,2"),
            fill = SolidColor(Color(0xFF000000)),
        )
        }.build().also { _Triangle = it }
    }

private var _Triangle: ImageVector? = null
