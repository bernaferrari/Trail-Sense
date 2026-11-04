package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/lizard.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Lizard: ImageVector
    get() {
        _Lizard?.let { return it }
        return ImageVector.Builder(
            name = "Lizard",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 357.96f,
            viewportHeight = 357.96f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M23.94,178.98c-0.81,-11.57 39.91,-18.75 66.24,-19.55c26.33,-0.79 40.31,5.59 40.31,9.58c0,4 -31.93,7.97 -32.33,18.35c-0.39,10.4 4.78,11.19 15.17,11.99c10.37,0.79 43.65,3.97 44.05,-3.21c0.4,-7.17 -34.08,-3.18 -34.08,-7.17c0,-3.99 40.31,-6.81 40.7,-22.76c13.17,7.19 61.11,41.9 101.02,41.9c24.78,0.44 24.93,-4 24.93,-7.59c0,-3.58 -4.58,-9.18 -26.93,-8.78c-22.34,0.4 -35.57,-9.18 -34.77,-12.77c0.8,-3.59 20.36,-0.01 33.52,0c38.92,0 96.18,-16.71 96.18,-48.65c0,-11.54 -19.95,-20.02 -45.9,-20.39c-26.73,-0.38 -27.13,16.77 -26.73,25.15c0.4,8.38 1.2,15.17 -3.19,15.56c-4.39,0.41 -33.96,-40.69 -121.32,-40.71C56.66,109.93 -0.4,135.48 0,177.38c0.4,41.91 203.52,70.65 203.52,70.65C169.2,233.25 24.74,190.54 23.94,178.98z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Lizard = it }
    }

private var _Lizard: ImageVector? = null
