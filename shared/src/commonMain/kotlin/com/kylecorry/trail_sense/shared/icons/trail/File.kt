package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_file.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.File: ImageVector
    get() {
        _File?.let { return it }
        return ImageVector.Builder(
            name = "File",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M6 2C4.9 2 4.01 2.9 4.01 4L4 20c0 1.1 0.89 2 1.99 2H18c1.1 0 2-0.9 2-2V8l-6-6H6zm7 7V3.5L18.5 9H13z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _File = it }
    }

private var _File: ImageVector? = null
