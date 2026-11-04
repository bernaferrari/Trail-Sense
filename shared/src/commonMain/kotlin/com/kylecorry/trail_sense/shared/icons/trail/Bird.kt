package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/bird.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Bird: ImageVector
    get() {
        _Bird?.let { return it }
        return ImageVector.Builder(
            name = "Bird",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 512.0f,
            viewportHeight = 512.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M365.94,432.83H341.85l-36.05,-36.05c31.32,-11.36 53.76,-41.42 53.76,-76.62v-54.87c0,-77.17 -91.04,-118.35 -149.04,-68.43L5.7,372.91c-11.48,9.87 -4.45,28.73 10.65,28.73h157.38l59.08,59.08c3.06,3.06 7.22,4.79 11.56,4.79h30.86c9.02,0 16.34,-7.32 16.34,-16.34s-7.32,-16.34 -16.34,-16.34h-24.09l-31.19,-31.19h44.5l59.08,59.08c3.06,3.06 7.22,4.79 11.55,4.79h30.86c9.02,0 16.34,-7.32 16.34,-16.34S374.96,432.83 365.94,432.83z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        addPath(
            pathData = addPathNodes("M437.45,116.86h-7.36c-17.17,-97.3 -175.22,-98.46 -175.22,26.42c73.61,-8.72 137.38,48.8 137.38,122.02v54.87c0,31.67 -12.96,60.36 -33.86,81.06c41.06,-4.16 73.21,-38.92 73.21,-81.06V207.75h64.06c9.02,0 16.34,-7.32 16.34,-16.34C512,150.3 478.55,116.86 437.45,116.86zM364.83,149.32c-11.94,0 -21.62,-9.68 -21.62,-21.62c0,-11.94 9.68,-21.62 21.62,-21.62s21.62,9.68 21.62,21.62C386.45,139.64 376.77,149.32 364.83,149.32z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Bird = it }
    }

private var _Bird: ImageVector? = null
