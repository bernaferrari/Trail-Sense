package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_map_group.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.MapGroup: ImageVector
    get() {
        _MapGroup?.let { return it }
        return ImageVector.Builder(
            name = "MapGroup",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M2.4 2.4c-1.32 0-2.388 1.081-2.388 2.401L0 19.199c0 1.32 1.08 2.401 2.4 2.401h19.2c1.32 0 2.4-1.081 2.4-2.401v-12c0-1.32-1.08-2.398-2.4-2.398H12L9.6 2.4H2.4zM10 6l4 1.4L17.561 6h0.105A0.333 0.333 0 0 1 18 6.334v10.08c0 0.153-0.1 0.252-0.24 0.318L14 18l-4-1.4-3.561 1.38C6.406 18 6.367 18 6.334 18A0.333 0.333 0 0 1 6 17.666V7.586c0-0.153 0.1-0.272 0.24-0.318L10 6zm0 1.334v7.926l4 1.406V8.74l-4-1.406z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _MapGroup = it }
    }

private var _MapGroup: ImageVector? = null
