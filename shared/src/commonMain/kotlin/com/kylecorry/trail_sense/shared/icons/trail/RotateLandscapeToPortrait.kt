package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_rotate_landscape_to_portrait.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.RotateLandscapeToPortrait: ImageVector
    get() {
        _RotateLandscapeToPortrait?.let { return it }
        return ImageVector.Builder(
            name = "RotateLandscapeToPortrait",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M15.273 0h6.545C23.023 0 24 0.977 24 2.182v14.182c0 1.205-0.977 2.181-2.182 2.181h-1.09v-3.272h1.09V2.182h-6.545v8.727H13.09V2.182C13.09 0.977 14.068 0 15.273 0M0 21.818v-6.545c0-1.205 0.977-2.182 2.182-2.182h14.182c1.205 0 2.181 0.977 2.181 2.182v6.545c0 1.205-0.976 2.182-2.181 2.182H2.182C0.977 24 0 23.023 0 21.818m15.273 0v-6.545H2.182v6.545h13.09M3.935 10.255h1.112A3.905 3.905 45 0 1 8.943 6.59v1.674l2.248-2.254-0.078-0.039-2.17-2.17v1.674a5.02 5.02 0 0 0-5.008 4.78z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _RotateLandscapeToPortrait = it }
    }

private var _RotateLandscapeToPortrait: ImageVector? = null
