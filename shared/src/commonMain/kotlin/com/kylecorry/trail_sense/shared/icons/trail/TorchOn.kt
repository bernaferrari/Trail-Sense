package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_torch_on.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.TorchOn: ImageVector
    get() {
        _TorchOn?.let { return it }
        return ImageVector.Builder(
            name = "TorchOn",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M7 2v11h3v9l7-12h-4l4-8z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _TorchOn = it }
    }

private var _TorchOn: ImageVector? = null
