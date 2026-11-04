package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_visitor_center.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.VisitorCenter: ImageVector
    get() {
        _VisitorCenter?.let { return it }
        return ImageVector.Builder(
            name = "VisitorCenter",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 22.0f,
            viewportHeight = 22.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M20 13l-7-4.7V4h2v1h3V1h-3V0h-3v7.7L11 7l-9 6H1v1h2v8h16v-8h2v-1h-1zm-7 6l-2 2H9l1-6H9l1-1h3l-1 1-0.7 4H13zm0-6h-2v-2h2v2z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _VisitorCenter = it }
    }

private var _VisitorCenter: ImageVector? = null
