package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/gem.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Gem: ImageVector
    get() {
        _Gem?.let { return it }
        return ImageVector.Builder(
            name = "Gem",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 16.0f,
            viewportHeight = 16.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M0,7L7,16H9L16,7V5L13,1H3L0,5V7ZM8,14H8.022L13.466,7H10.625L8,14ZM10.25,5H13.5L12,3H8.75L10.25,5Z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
            pathFillType = PathFillType.EvenOdd,
        )
        }.build().also { _Gem = it }
    }

private var _Gem: ImageVector? = null
