package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/cave.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Cave: ImageVector
    get() {
        _Cave?.let { return it }
        return ImageVector.Builder(
            name = "Cave",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M10.175,1.131 L8.482,4.395 5.363,3.562 3.828,8.006 1.556,10.853 1.238,16.064 0.394,22.869L6.111,22.869l-0.034,-2.081 0.691,-3.028 1.328,-2.016 1.804,-2.177 3.08,0.743 2.337,0.797 1.222,2.283 1.222,2.601 0.932,2.878h4.913L23.342,15.367 21.492,11.133 20.011,11.48 18.373,7.312 15.727,5.089 14.775,1.548Z"),
            fill = SolidColor(Color(0xFF000000)),
            stroke = SolidColor(Color(0x00000000)),
            strokeLineWidth = 0.7561f,
            strokeLineCap = StrokeCap.Round,
        )
        }.build().also { _Cave = it }
    }

private var _Cave: ImageVector? = null
