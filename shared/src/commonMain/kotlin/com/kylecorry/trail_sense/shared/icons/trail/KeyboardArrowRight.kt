package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_keyboard_arrow_right.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.KeyboardArrowRight: ImageVector
    get() {
        _KeyboardArrowRight?.let { return it }
        return ImageVector.Builder(
            name = "KeyboardArrowRight",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M8.59 16.59L13.17 12 8.59 7.41 10 6l6 6-6 6-1.41-1.41z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _KeyboardArrowRight = it }
    }

private var _KeyboardArrowRight: ImageVector? = null
