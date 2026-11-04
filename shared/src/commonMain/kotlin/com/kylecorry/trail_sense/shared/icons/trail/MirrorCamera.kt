package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_mirror_camera.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.MirrorCamera: ImageVector
    get() {
        _MirrorCamera?.let { return it }
        return ImageVector.Builder(
            name = "MirrorCamera",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M8.29 10.28L11.53 7.03L12.59 8.09L9.35 11.34L8.29 10.28M8.7 14.61L14.36 8.95L15.42 10L9.76 15.67L8.7 14.61M18 3V21H6V3H18M20 1H4V23H20V1Z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _MirrorCamera = it }
    }

private var _MirrorCamera: ImageVector? = null
