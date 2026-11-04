package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/metal.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Metal: ImageVector
    get() {
        _Metal?.let { return it }
        return ImageVector.Builder(
            name = "Metal",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M9 5v5c4.03 2.47-0.56 4.97-3 6v3h15v-3c-6.41-2.73-3.53-7 1-8V5H9M2 6c0.81 2.13 2.42 3.5 5 4V6H2z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Metal = it }
    }

private var _Metal: ImageVector? = null
