package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_elevation_loss.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.ElevationLoss: ImageVector
    get() {
        _ElevationLoss?.let { return it }
        return ImageVector.Builder(
            name = "ElevationLoss",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M14.922 5.078c-0.814 0-7.66 10.645-9.078 13.844H24c-1.42-3.199-8.265-13.844-9.078-13.844zM13.902 8h2.039v4.632h1.143L14.922 16l-2.162-3.368h1.143V8z"),
            fill = SolidColor(Color(0xFF666666)),
        )
        addPath(
            pathData = addPathNodes("M6.543 8.063c-0.578 0-5.265 7.963-6.543 10.859h13.085C11.806 16.026 7.119 8.063 6.542 8.063z"),
            fill = SolidColor(Color(0xFF808080)),
        )
        }.build().also { _ElevationLoss = it }
    }

private var _ElevationLoss: ImageVector? = null
