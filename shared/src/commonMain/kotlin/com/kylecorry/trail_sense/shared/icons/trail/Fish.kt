package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/fish.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Fish: ImageVector
    get() {
        _Fish?.let { return it }
        return ImageVector.Builder(
            name = "Fish",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 512.0f,
            viewportHeight = 512.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M376.49,156.87c-7.82,-2.62 -15.81,-4.8 -23.91,-6.56c-11.36,-35.8 -44.72,-60.76 -83.22,-60.76c-9.02,0 -16.34,7.32 -16.34,16.34v45.24c-61.87,14.49 -114.69,54.45 -145.69,109.98l-82.82,-47.81C13.64,207.02 0,214.87 0,227.44v112.83c0,12.66 13.71,20.39 24.51,14.15l82.77,-47.79c54.86,99.11 169.47,137 269.27,104.38C347.09,328.88 347.04,239.12 376.49,156.87zM283.5,295.32c9.02,0 16.34,7.32 16.34,16.34c0,9.02 -7.32,16.34 -16.34,16.34c-39.67,0 -71.94,-32.27 -71.94,-71.94c0,-9.02 7.32,-16.34 16.34,-16.34h55.6c9.02,0 16.34,7.32 16.34,16.34c0,9.02 -7.32,16.34 -16.34,16.34h-35.69C254.02,285.91 267.68,295.32 283.5,295.32z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        addPath(
            pathData = addPathNodes("M510.51,277.04c-21.97,-47.82 -59.16,-85.03 -103.82,-107.57c-26.49,75.13 -25.77,155.97 0.13,229.15c46.97,-23.35 82.42,-61.66 103.69,-107.95C512.5,286.35 512.5,281.37 510.51,277.04zM440.12,273.83c-11.94,0 -21.62,-9.68 -21.62,-21.62c0,-11.94 9.68,-21.62 21.62,-21.62s21.62,9.68 21.62,21.62C461.74,264.15 452.06,273.83 440.12,273.83z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Fish = it }
    }

private var _Fish: ImageVector? = null
