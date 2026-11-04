package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_triangle_up.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.TriangleUp: ImageVector
    get() {
        _TriangleUp?.let { return it }
        return ImageVector.Builder(
            name = "TriangleUp",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M12,3L22.39,21H1.61Z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _TriangleUp = it }
    }

private var _TriangleUp: ImageVector? = null
