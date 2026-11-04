package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/level.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Level: ImageVector
    get() {
        _Level?.let { return it }
        return ImageVector.Builder(
            name = "Level",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M22 8H2v8h20V8m-4 6v-4h2v4h-2m-7-2h2c1.1 0 2-0.89 2-2h2v4H7v-4h2c0 1.11 0.9 2 2 2m-7 2v-4h2v4H4z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _Level = it }
    }

private var _Level: ImageVector? = null
