package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_sun.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Sun: ImageVector
    get() {
        _Sun?.let { return it }
        return ImageVector.Builder(
            name = "Sun",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 178.4048f,
            viewportHeight = 178.4048f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M0 89.202a89.202 89.202 0 1 1 178.405 0A89.202 89.202 0 1 1 0 89.202"),
            fill = SolidColor(Color(0xFFFFDD55)),
        )
        }.build().also { _Sun = it }
    }

private var _Sun: ImageVector? = null
