package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/planet_netpune.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.PlanetNetpune: ImageVector
    get() {
        _PlanetNetpune?.let { return it }
        return ImageVector.Builder(
            name = "PlanetNetpune",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 122.68f,
            viewportHeight = 122.68f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M61.34,61.34m-61.34,0a61.34,61.34 0,1 1,122.68 0a61.34,61.34 0,1 1,-122.68 0"),
            fill = SolidColor(Color(0xFF3771C8)),
            stroke = SolidColor(Color(0x00000000)),
            strokeLineWidth = 5.0f,
            strokeAlpha = 0.498f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Miter,
        )
        addPath(
            pathData = addPathNodes("M53.45,71.56a17.11,5.41 0,1 0,34.21 0a17.11,5.41 0,1 0,-34.21 0z"),
            fill = SolidColor(Color(0xFF003380)),
            fillAlpha = 0.4824f,
            stroke = SolidColor(Color(0x00000000)),
            strokeLineWidth = 5.0f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Miter,
        )
        }.build().also { _PlanetNetpune = it }
    }

private var _PlanetNetpune: ImageVector? = null
