package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_arrow_target.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ArrowTarget: ImageVector
    get() {
        _ArrowTarget?.let { return it }
        return ImageVector.Builder(
            name = "ArrowTarget",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M0 18h24L12 6z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _ArrowTarget = it }
    }

private var _ArrowTarget: ImageVector? = null
