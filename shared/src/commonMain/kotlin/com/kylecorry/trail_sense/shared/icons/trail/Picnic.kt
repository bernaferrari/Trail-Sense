package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_picnic.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Picnic: ImageVector
    get() {
        _Picnic?.let { return it }
        return ImageVector.Builder(
            name = "Picnic",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M24 11.2h-5.6L16 5.6h4V3.2H4v2.4h4l-2.4 5.6H0v2.4h4.8L2.4 20h2.4l2.4-6.4h9.6l2.4 6.4h2.4l-2.4-6.4H24zm-16 0l2.4-5.6h3.2l2.4 5.6z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Picnic = it }
    }

private var _Picnic: ImageVector? = null
