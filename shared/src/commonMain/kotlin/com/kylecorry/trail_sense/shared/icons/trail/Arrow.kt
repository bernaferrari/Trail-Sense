package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_arrow.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Arrow: ImageVector
    get() {
        _Arrow?.let { return it }
        return ImageVector.Builder(
            name = "Arrow",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M15 20H9v-8H4.16L12 4.16 19.84 12H15v8z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Arrow = it }
    }

private var _Arrow: ImageVector? = null
