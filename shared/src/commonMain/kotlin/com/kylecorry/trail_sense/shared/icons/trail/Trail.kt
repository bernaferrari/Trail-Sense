package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_trail.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Trail: ImageVector
    get() {
        _Trail?.let { return it }
        return ImageVector.Builder(
            name = "Trail",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M13.5 5.5c1.1 0 2-0.9 2-2s-0.9-2-2-2-2 0.9-2 2 0.9 2 2 2zm4 5.28c-1.23-0.37-2.22-1.17-2.8-2.18l-1-1.6c-0.41-0.65-1.11-1-1.84-1-0.78 0-1.59 0.5-1.78 1.44S7 23 7 23h2.1l1.8-8 2.1 2v6h2v-7.5l-2.1-2 0.6-3c1 1.15 2.41 2.01 4 2.34V23H19V9h-1.5v1.78zM7.43 13.13l-2.12-0.41c-0.54-0.11-0.9-0.63-0.79-1.17l0.76-3.93c0.21-1.08 1.26-1.79 2.34-1.58l1.16 0.23-1.35 6.86z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Trail = it }
    }

private var _Trail: ImageVector? = null
