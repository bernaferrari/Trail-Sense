package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_tool_white_noise.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ToolWhiteNoise: ImageVector
    get() {
        _ToolWhiteNoise?.let { return it }
        return ImageVector.Builder(
            name = "ToolWhiteNoise",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M23 12h-6v-2l3.39-4H17V4h6v2l-3.38 4H23v2m-8 4H9v-2l3.39-4H9V8h6v2l-3.38 4H15v2m-8 4H1v-2l3.39-4H1v-2h6v2l-3.38 4H7v2z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _ToolWhiteNoise = it }
    }

private var _ToolWhiteNoise: ImageVector? = null
