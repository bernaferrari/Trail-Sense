package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_arrow_up.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ArrowUp: ImageVector
    get() {
        _ArrowUp?.let { return it }
        return ImageVector.Builder(
            name = "ArrowUp",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _ArrowUp = it }
    }

private var _ArrowUp: ImageVector? = null
