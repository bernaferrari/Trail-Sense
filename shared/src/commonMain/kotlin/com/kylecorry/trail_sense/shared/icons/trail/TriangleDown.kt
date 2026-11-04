package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_triangle_down.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.TriangleDown: ImageVector
    get() {
        _TriangleDown?.let { return it }
        return ImageVector.Builder(
            name = "TriangleDown",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M12,21L22.39,3H1.61Z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _TriangleDown = it }
    }

private var _TriangleDown: ImageVector? = null
