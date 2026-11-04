package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_help_simple.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.HelpSimple: ImageVector
    get() {
        _HelpSimple?.let { return it }
        return ImageVector.Builder(
            name = "HelpSimple",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M10 19h3v3h-3v-3m2-17c5.35 0.22 7.68 5.62 4.5 9.67-0.83 1-2.17 1.66-2.83 2.5C13 15 13 16 13 17h-3c0-1.67 0-3.08 0.67-4.08 0.66-1 2-1.59 2.83-2.25C15.92 8.43 15.32 5.26 12 5a3 3 0 0 0-3 3H6a6 6 0 0 1 6-6z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _HelpSimple = it }
    }

private var _HelpSimple: ImageVector? = null
