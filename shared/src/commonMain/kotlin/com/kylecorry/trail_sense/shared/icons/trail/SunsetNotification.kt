package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_sunset_notification.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.SunsetNotification: ImageVector
    get() {
        _SunsetNotification?.let { return it }
        return ImageVector.Builder(
            name = "SunsetNotification",
            defaultWidth = 24.0.dp,
            defaultHeight = 23.9999.dp,
            viewportWidth = 178.4051f,
            viewportHeight = 178.4046f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M89.202 0A89.202 89.202 0 0 0 0 89.202a89.202 89.202 0 0 0 89.202 89.203 89.202 89.202 135 0 0 89.203-89.203A89.202 89.202 135 0 0 89.202 0zM75.146 34.029h28.112v63.889h15.763l-29.819 46.458-29.819-46.457h15.763z"),
            fill = SolidColor(Color(0xFFFFDD55)),
        )
        }.build().also { _SunsetNotification = it }
    }

private var _SunsetNotification: ImageVector? = null
