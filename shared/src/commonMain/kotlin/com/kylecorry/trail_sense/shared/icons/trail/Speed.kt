package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_speed.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Speed: ImageVector
    get() {
        _Speed?.let { return it }
        return ImageVector.Builder(
            name = "Speed",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M12 16a3 3 0 0 1-3-3c0-1.12 0.61-2.1 1.5-2.61l9.71-5.62-5.53 9.58C14.18 15.33 13.17 16 12 16m0-13c1.81 0 3.5 0.5 4.97 1.32l-2.1 1.21C14 5.19 13 5 12 5a8 8 0 0 0-8 8c0 2.21 0.89 4.21 2.34 5.65h0.01c0.39 0.39 0.39 1.02 0 1.41-0.39 0.39-1.03 0.39-1.42 0.01C3.12 18.26 2 15.76 2 13A10 10 0 0 1 12 3m10 10c0 2.76-1.12 5.26-2.93 7.07-0.39 0.38-1.02 0.38-1.41-0.01-0.39-0.39-0.39-1.02 0-1.41C19.11 17.2 20 15.21 20 13c0-1-0.19-2-0.54-2.9L20.67 8C21.5 9.5 22 11.18 22 13z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Speed = it }
    }

private var _Speed: ImageVector? = null
