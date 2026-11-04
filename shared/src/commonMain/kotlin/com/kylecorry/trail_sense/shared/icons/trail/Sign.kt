package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_sign.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Sign: ImageVector
    get() {
        _Sign?.let { return it }
        return ImageVector.Builder(
            name = "Sign",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 30.0f,
            viewportHeight = 30.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M14 2H6C5.5 2 5 2.4 5 3v11c0 0.6 0.4 1 1 1h8v13h-1l-1 1v1h6v-1l-1-1h-1V15h8c0.5 0 1-0.4 1-1V3c0-0.6-0.4-1-1-1h-8V1c0-0.6-0.4-1-1-1s-1 0.4-1 1v1zM8 5h14v1H8V5zm0 3h14v1H8V8zm0 3h14v1H8v-1z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Sign = it }
    }

private var _Sign: ImageVector? = null
