package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_phone_portrait.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.PhonePortrait: ImageVector
    get() {
        _PhonePortrait?.let { return it }
        return ImageVector.Builder(
            name = "PhonePortrait",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M17 1.01L7 1C5.9 1 5 1.9 5 3v18c0 1.1 0.9 2 2 2h10c1.1 0 2-0.9 2-2V3c0-1.1-0.9-1.99-2-1.99zM17 19H7V5h10v14z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _PhonePortrait = it }
    }

private var _PhonePortrait: ImageVector? = null
