package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_datum.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Datum: ImageVector
    get() {
        _Datum?.let { return it }
        return ImageVector.Builder(
            name = "Datum",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M5 3C3.89 3 3 3.89 3 5v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V5c0-1.11-0.9-2-2-2H5m10.78 2H19v12.18c-0.26-0.8-1.31-1.39-2.2-1.39h-1v-3a1 1 0 0 0-1-1h-6v-2h2a1 1 0 0 0 1-1v-2h2c1.03 0 1.87-0.79 1.98-1.79M5 10.29l4.8 4.5v1c0 1.11 0.9 2 2 2V19H5v-8.71z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Datum = it }
    }

private var _Datum: ImageVector? = null
