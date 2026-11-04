package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_compass_unavailable.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.CompassUnavailable: ImageVector
    get() {
        _CompassUnavailable?.let { return it }
        return ImageVector.Builder(
            name = "CompassUnavailable",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        group(scaleX = 1.0f, scaleY = 1.0f, translationX = 0.0f, translationY = 0.0f) {
            addPath(
                pathData = addPathNodes("M14.19,14.19L6,18L9.81,9.81L18,6M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12A10,10 0 0,0 12,2M12,10.9A1.1,1.1 0 0,0 10.9,12A1.1,1.1 0 0,0 12,13.1A1.1,1.1 0 0,0 13.1,12A1.1,1.1 0 0,0 12,10.9Z"),
                fill = SolidColor(Color(0xFFFF6D00)),
            )
        }
        group(scaleX = 1.0f, scaleY = 1.0f, translationX = 0.0f, translationY = 0.0f) {
            addPath(
                pathData = addPathNodes("M3,3L21,21"),
                stroke = SolidColor(Color(0xFFFFFFFF)),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
            )
        }
        }.build().also { _CompassUnavailable = it }
    }

private var _CompassUnavailable: ImageVector? = null
