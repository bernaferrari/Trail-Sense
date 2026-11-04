package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_barn.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Barn: ImageVector
    get() {
        _Barn?.let { return it }
        return ImageVector.Builder(
            name = "Barn",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M24 7.8C23.6 4.5 20.9 2 17.5 2c-1.7 0-3.4 0.7-4.6 1.9-0.7 0.7-1.2 1.4-1.5 2.3L17 9.9V10h3v2h-3v2h3v2h-3v2h3v2h-3v2h7V7.8M13.3 7c0.6-1.8 2.3-3 4.2-3s3.6 1.2 4.2 3h-8.4M0 11v11h5v-7h5v7h5V11L7.5 6 0 11z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Barn = it }
    }

private var _Barn: ImageVector? = null
