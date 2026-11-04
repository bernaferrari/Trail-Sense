package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/planet_uranus.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.PlanetUranus: ImageVector
    get() {
        _PlanetUranus?.let { return it }
        return ImageVector.Builder(
            name = "PlanetUranus",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 122.68f,
            viewportHeight = 122.68f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M61.34,61.34m-61.34,0a61.34,61.34 0,1 1,122.68 0a61.34,61.34 0,1 1,-122.68 0"),
            fill = SolidColor(Color(0xFF5FBCD3)),
            stroke = SolidColor(Color(0x00000000)),
            strokeLineWidth = 5.0f,
            strokeAlpha = 0.498f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Miter,
        )
        }.build().also { _PlanetUranus = it }
    }

private var _PlanetUranus: ImageVector? = null
