package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_notification.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Notification: ImageVector
    get() {
        _Notification?.let { return it }
        return ImageVector.Builder(
            name = "Notification",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M12 22c1.1 0 2-0.9 2-2h-4c0 1.1 0.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-0.83-0.67-1.5-1.5-1.5S10.5 3.17 10.5 4v0.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Notification = it }
    }

private var _Notification: ImageVector? = null
