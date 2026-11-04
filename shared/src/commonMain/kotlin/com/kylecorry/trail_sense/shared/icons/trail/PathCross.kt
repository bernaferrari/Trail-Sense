package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/path_cross.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.PathCross: ImageVector
    get() {
        _PathCross?.let { return it }
        return ImageVector.Builder(
            name = "PathCross",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M0 10.237L1.763 12 0 13.763 1.237 15 3 13.237 4.763 15 6 13.763 4.237 12 6 10.237 4.763 9 3 10.763 1.237 9zm9 0L10.763 12 9 13.763 10.237 15 12 13.237 13.763 15 15 13.763 13.237 12 15 10.237 13.763 9 12 10.763 10.237 9zm9 0L19.763 12 18 13.763 19.237 15 21 13.237 22.763 15 24 13.763 22.237 12 24 10.237 22.763 9 21 10.763 19.237 9z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _PathCross = it }
    }

private var _PathCross: ImageVector? = null
