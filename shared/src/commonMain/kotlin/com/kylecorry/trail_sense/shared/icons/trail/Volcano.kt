package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/volcano.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Volcano: ImageVector
    get() {
        _Volcano?.let { return it }
        return ImageVector.Builder(
            name = "Volcano",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M18 8H11L9 13H6L2 22H22L18 8M13 1H15V5H13V1M16.12 5.47L18.95 2.64L20.36 4.05L17.54 6.88L16.12 5.47M7.64 4.05L9.05 2.64L11.88 5.46L10.47 6.88L7.64 4.05Z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Volcano = it }
    }

private var _Volcano: ImageVector? = null
