package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_climbing.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Climbing: ImageVector
    get() {
        _Climbing?.let { return it }
        return ImageVector.Builder(
            name = "Climbing",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M2.683 10.232l-0.24 0.72v0.4l0.08 6.078-2.079 4.479c-0.08 0.16-0.16 0.32-0.16 0.48-0.08 0.8 0.4 1.44 1.2 1.6 0.56 0.08 1.12-0.24 1.36-0.72l2.319-4.959c0.08-0.08 0.08-0.24 0.08-0.4v-3.919l3.519 1.52 0.56 3.679c0.08 0.56 0.56 0.96 1.12 1.04 0.72 0.08 1.44-0.4 1.52-1.12v-0.4l-0.642-4.319c-0.08-0.4-0.4-0.8-0.8-0.96l-3.199-1.44L9.32 8.472l0.96 1.28c0.08 0.08 0.24 0.24 0.4 0.24l3.839 1.12c0.48 0.08 1.04-0.16 1.2-0.64 0.24-0.56-0.08-1.2-0.56-1.44h-0.08L11.8 8.072 9.321 5.113c-0.4-0.4-0.88-0.64-1.44-0.799-0.96-0.16-1.919 0.319-2.399 1.199z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        addPath(
            pathData = addPathNodes("M13.64 18.949l-0.24 1.839-6.558 1.6 0.08 1.12h15.995c0.56 0 1.04-0.48 1.04-1.04L23.956 0.955l-2.159-0.48-1.28 5.678-2.159 0.8-1.839 5.198 1.759 3.999-0.48 1.52zM7.181 2.319a1.999 1.999 112.524 1 1 3.947-0.641 1.999 1.999 112.524 1 1-3.947 0.641"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        addPath(
            pathData = addPathNodes("M5.162 4.394c0.16-0.24 0.08-0.64-0.24-0.8l-0.96-0.56c-0.32-0.16-0.639-0.08-0.799 0.24l-3.04 5.278c-0.159 0.24-0.079 0.64 0.24 0.8l0.96 0.56c0.24 0.16 0.64 0.08 0.8-0.24z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Climbing = it }
    }

private var _Climbing: ImageVector? = null
