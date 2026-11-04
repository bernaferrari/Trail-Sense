package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/declination.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Declination: ImageVector
    get() {
        _Declination?.let { return it }
        return ImageVector.Builder(
            name = "Declination",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("m16.325,22.016 l-9.841,-17.894l0,5.804"),
            stroke = SolidColor(Color(0xFFFF6D00)),
            strokeLineWidth = 1.5f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
        addPath(
            pathData = addPathNodes("m16.415,22.063l0,-16.486"),
            stroke = SolidColor(Color(0xFFFF6D00)),
            strokeLineWidth = 1.348f,
            strokeLineCap = StrokeCap.Round,
        )
        addPath(
            pathData = addPathNodes("m16.415,4.06 l1.144,0.69 -0.304,-1.301 1.01,-0.875 -1.331,-0.115 -0.52,-1.225 -0.52,1.225 -1.331,0.115 1.009,0.875 -0.302,1.301z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _Declination = it }
    }

private var _Declination: ImageVector? = null
