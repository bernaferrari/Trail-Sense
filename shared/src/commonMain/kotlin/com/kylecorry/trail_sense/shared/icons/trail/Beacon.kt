package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_beacon.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Beacon: ImageVector
    get() {
        _Beacon?.let { return it }
        return ImageVector.Builder(
            name = "Beacon",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M12 2L4.5 20.29 5.21 21 12 18l6.79 3 0.71-0.71z"),
            fill = SolidColor(Color(0xFF000000)),
        )
        }.build().also { _Beacon = it }
    }

private var _Beacon: ImageVector? = null
