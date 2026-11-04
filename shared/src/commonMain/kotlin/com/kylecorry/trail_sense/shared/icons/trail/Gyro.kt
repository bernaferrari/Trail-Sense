package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_gyro.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Gyro: ImageVector
    get() {
        _Gyro?.let { return it }
        return ImageVector.Builder(
            name = "Gyro",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = true
        ).apply {
        addPath(
            pathData = addPathNodes("M12 7C6.48 7 2 9.24 2 12c0 2.24 2.94 4.13 7 4.77V20l4-4-4-4v2.73c-3.15-0.56-5-1.9-5-2.73 0-1.06 3.04-3 8-3s8 1.94 8 3c0 0.73-1.46 1.89-4 2.53v2.05c3.53-0.77 6-2.53 6-4.58 0-2.76-4.48-5-10-5z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Gyro = it }
    }

private var _Gyro: ImageVector? = null
