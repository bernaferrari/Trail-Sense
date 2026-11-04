package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_altitude.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Altitude: ImageVector
    get() {
        _Altitude?.let { return it }
        return ImageVector.Builder(
            name = "Altitude",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M14.921 5.078c-0.813 0-7.658 10.645-9.077 13.844H24c-1.42-3.199-8.265-13.844-9.079-13.844z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        addPath(
            pathData = addPathNodes("M6.543 8.063c-0.578 0-5.265 7.963-6.543 10.859h13.085C11.806 16.026 7.119 8.063 6.542 8.063z"),
            fill = SolidColor(Color(0xFFFD750E)),
        )
        }.build().also { _Altitude = it }
    }

private var _Altitude: ImageVector? = null
