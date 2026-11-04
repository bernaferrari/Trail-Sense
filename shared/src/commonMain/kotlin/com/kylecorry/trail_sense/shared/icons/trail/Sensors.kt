package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_sensors.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Sensors: ImageVector
    get() {
        _Sensors?.let { return it }
        return ImageVector.Builder(
            name = "Sensors",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M15 9H9v6h6V9zm-2 4h-2v-2h2v2zm8-2V9h-2V7c0-1.1-0.9-2-2-2h-2V3h-2v2h-2V3H9v2H7C5.9 5 5 5.9 5 7v2H3v2h2v2H3v2h2v2c0 1.1 0.9 2 2 2h2v2h2v-2h2v2h2v-2h2c1.1 0 2-0.9 2-2v-2h2v-2h-2v-2h2zm-4 6H7V7h10v10z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _Sensors = it }
    }

private var _Sensors: ImageVector? = null
