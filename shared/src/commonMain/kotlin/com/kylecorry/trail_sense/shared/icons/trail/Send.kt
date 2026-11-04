package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_send.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Send: ImageVector
    get() {
        _Send?.let { return it }
        return ImageVector.Builder(
            name = "Send",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = true
        ).apply {
        addPath(
            pathData = addPathNodes("M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _Send = it }
    }

private var _Send: ImageVector? = null
