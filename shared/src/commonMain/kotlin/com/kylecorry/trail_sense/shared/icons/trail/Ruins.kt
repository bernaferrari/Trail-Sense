package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_ruins.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Ruins: ImageVector
    get() {
        _Ruins?.let { return it }
        return ImageVector.Builder(
            name = "Ruins",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M9.109 0.164l-6.494 4.33 0.175 1.014-0.726 0.799-0.29 1.235-1.017 0.799-0.756 0.101v15.395h8v-11.2h8v11.2L24 23.836V9.589l-0.37-0.37-1.659-1.66-0.305-1.14-1.479-0.396-0.616 0.356-0.766 0.766-2.858-2.858-2.114 1.22-1.306-1.306 0.373-1.393-1.471-1.471-2.318 1.338z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Ruins = it }
    }

private var _Ruins: ImageVector? = null
