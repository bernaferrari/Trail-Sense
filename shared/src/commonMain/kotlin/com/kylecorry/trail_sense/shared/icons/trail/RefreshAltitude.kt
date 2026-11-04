package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/refresh_altitude.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.RefreshAltitude: ImageVector
    get() {
        _RefreshAltitude?.let { return it }
        return ImageVector.Builder(
            name = "RefreshAltitude",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M14.922,5.078C14.108,5.078 7.263,15.724 5.844,18.922L24,18.922C22.581,15.724 15.735,5.078 14.922,5.078zM15.173,10.36C15.945,10.36 16.64,10.675 17.147,11.181L17.968,10.36L17.968,12.805L15.523,12.805L16.647,11.68C16.27,11.3 15.753,11.059 15.173,11.059A2.096,2.096 0,0 0,13.077 13.155A2.096,2.096 0,0 0,15.173 15.251C16.085,15.251 16.86,14.667 17.147,13.853L17.873,13.853C17.562,15.059 16.476,15.95 15.173,15.95A2.795,2.795 0,0 1,12.378 13.155A2.795,2.795 0,0 1,15.173 10.36z"),
            fill = SolidColor(Color(0xFF666666)),
            stroke = SolidColor(Color(0x00000000)),
            strokeLineWidth = 5.0f,
            strokeAlpha = 0.498f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Miter,
        )
        addPath(
            pathData = addPathNodes("m6.542,8.063c-0.577,0 -5.264,7.963 -6.542,10.858L13.085,18.922C11.807,16.026 7.12,8.063 6.542,8.063Z"),
            fill = SolidColor(Color(0xFF808080)),
            stroke = SolidColor(Color(0x00000000)),
            strokeLineWidth = 5.0f,
            strokeAlpha = 0.498f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Miter,
        )
        }.build().also { _RefreshAltitude = it }
    }

private var _RefreshAltitude: ImageVector? = null
