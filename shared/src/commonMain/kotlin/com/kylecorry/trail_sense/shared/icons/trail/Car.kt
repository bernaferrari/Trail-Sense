package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_car.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Car: ImageVector
    get() {
        _Car?.let { return it }
        return ImageVector.Builder(
            name = "Car",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11C5.84 5 5.29 5.42 5.08 6.01L3 12v8c0 0.55 0.45 1 1 1h1c0.55 0 1-0.45 1-1v-1h12v1c0 0.55 0.45 1 1 1h1c0.55 0 1-0.45 1-1v-8l-2.08-5.99zM6.5 16C5.67 16 5 15.33 5 14.5S5.67 13 6.5 13 8 13.67 8 14.5 7.33 16 6.5 16zm11 0c-0.83 0-1.5-0.67-1.5-1.5s0.67-1.5 1.5-1.5 1.5 0.67 1.5 1.5-0.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Car = it }
    }

private var _Car: ImageVector? = null
