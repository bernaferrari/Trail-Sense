package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_undo.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Undo: ImageVector
    get() {
        _Undo?.let { return it }
        return ImageVector.Builder(
            name = "Undo",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M12.5 8c-2.65 0-5.05 0.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-0.78C21.08 11.03 17.15 8 12.5 8z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _Undo = it }
    }

private var _Undo: ImageVector? = null
