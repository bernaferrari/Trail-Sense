package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/bubble.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Bubble: ImageVector
    get() {
        _Bubble?.let { return it }
        return ImageVector.Builder(
            name = "Bubble",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M12 2A10 10 0 1 1 2 12 10 10 0 0 1 12 2z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Bubble = it }
    }

private var _Bubble: ImageVector? = null
