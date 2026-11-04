package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_share.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Share: ImageVector
    get() {
        _Share?.let { return it }
        return ImageVector.Builder(
            name = "Share",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M18 16.08c-0.76 0-1.44 0.3-1.96 0.77L8.91 12.7C8.96 12.47 9 12.24 9 12s-0.04-0.47-0.09-0.7l7.05-4.11C16.5 7.69 17.21 8 18 8c1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 0.24 0.04 0.47 0.09 0.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c0.79 0 1.5-0.31 2.04-0.81l7.12 4.16c-0.05 0.21-0.08 0.43-0.08 0.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Share = it }
    }

private var _Share: ImageVector? = null
