package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_beach.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Beach: ImageVector
    get() {
        _Beach?.let { return it }
        return ImageVector.Builder(
            name = "Beach",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M13.127 14.56l1.43-1.43 6.44 6.443L19.57 21zm4.293-5.73l2.86-2.86C16.33 2.02 9.93 2.01 5.98 5.95c3.93-1.3 8.31-0.25 11.44 2.88zM5.95 5.98c-3.94 3.95-3.93 10.35 0.02 14.3l2.86-2.86C5.7 14.29 4.65 9.91 5.95 5.98zm0.02-0.02L5.96 5.97c-0.38 3.01 1.17 6.88 4.3 10.02l5.73-5.73c-3.13-3.13-7.01-4.68-10.02-4.3z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Beach = it }
    }

private var _Beach: ImageVector? = null
