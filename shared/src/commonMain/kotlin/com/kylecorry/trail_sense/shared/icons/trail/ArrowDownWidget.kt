package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_arrow_down_widget.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ArrowDownWidget: ImageVector
    get() {
        _ArrowDownWidget?.let { return it }
        return ImageVector.Builder(
            name = "ArrowDownWidget",
            defaultWidth = 16.0.dp,
            defaultHeight = 16.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z"),
            fill = SolidColor(Color(0xFF000000)),
        )
        }.build().also { _ArrowDownWidget = it }
    }

private var _ArrowDownWidget: ImageVector? = null
