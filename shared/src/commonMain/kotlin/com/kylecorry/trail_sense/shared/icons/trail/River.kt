package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_river.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.River: ImageVector
    get() {
        _River?.let { return it }
        return ImageVector.Builder(
            name = "River",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M17.394 2.124l-1.179-0.442c-1.474 0.147-3.391 1.179-4.423 1.474C5.896 6.104 3.096 7.43 2.211 7.873 0.884 8.61 0 10.084 1.474 11.116c1.474 1.031 11.203 1.474 10.613 3.685C11.498 17.012 2.653 21.729 0 22.318h17.689s5.749-4.717 6.044-6.044c1.032-3.39-1.032-4.717-4.275-5.307-3.096-0.588-8.255-1.473-8.845-1.915-0.589-0.295-1.179-0.885-0.737-1.474 2.654-2.359 6.044-4.717 7.518-5.454z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _River = it }
    }

private var _River: ImageVector? = null
