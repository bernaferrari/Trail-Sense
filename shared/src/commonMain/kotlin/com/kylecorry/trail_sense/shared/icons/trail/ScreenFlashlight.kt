package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_screen_flashlight.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ScreenFlashlight: ImageVector
    get() {
        _ScreenFlashlight?.let { return it }
        return ImageVector.Builder(
            name = "ScreenFlashlight",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        group(scaleX = 1.0f, scaleY = 1.0f, translationX = 0.0f, translationY = 0.0f) {
            addPath(
                pathData = addPathNodes("M17 1.01L7 1C5.9 1 5 1.9 5 3v18c0 1.1 0.9 2 2 2h10c1.1 0 2-0.9 2-2V3c0-1.1-0.9-1.99-2-1.99zM17 19H7V5h10v14z"),
                fill = SolidColor(Color(0xFFFFFFFF)),
            )
        }
        group(scaleX = 0.4167f, scaleY = 0.4167f, translationX = 7.0f, translationY = 7.0f) {
            addPath(
                pathData = addPathNodes("M9 10L6 5h12l-3 5H9m9-6H6V2h12v2M9 22V11h6v11H9m3-9a1 1 0 0 0-1 1 1 1 0 0 0 1 1 1 1 0 0 0 1-1 1 1 0 0 0-1-1z"),
                fill = SolidColor(Color(0xFFFF6D00)),
            )
        }
        }.build().also { _ScreenFlashlight = it }
    }

private var _ScreenFlashlight: ImageVector? = null
