package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_sunrise_quick_action.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.SunriseQuickAction: ImageVector
    get() {
        _SunriseQuickAction?.let { return it }
        return ImageVector.Builder(
            name = "SunriseQuickAction",
            defaultWidth = 24.0.dp,
            defaultHeight = 23.9999.dp,
            viewportWidth = 178.41f,
            viewportHeight = 178.4f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M89.2 178.4a89.2 89.2 0 0 0 89.2-89.2A89.2 89.2 0 0 0 89.2 0 89.2 89.2 0 0 0 0 89.2a89.2 89.2 0 0 0 89.2 89.2zm14.06-34.02H75.15V80.49H59.39C69.33 65 79.27 49.52 89.21 34.03c9.94 15.49 19.88 30.97 29.82 46.46h-15.76z"),
            fill = SolidColor(Color(0xFFFFDD55)),
        )
        }.build().also { _SunriseQuickAction = it }
    }

private var _SunriseQuickAction: ImageVector? = null
