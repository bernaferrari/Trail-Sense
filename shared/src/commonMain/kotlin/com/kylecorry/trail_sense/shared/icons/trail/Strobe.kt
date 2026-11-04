package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/ic_strobe.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Strobe: ImageVector
    get() {
        _Strobe?.let { return it }
        return ImageVector.Builder(
            name = "Strobe",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M9.41 12.415L6.818 8.098H17.18l-2.59 4.317H9.409m7.772-5.181H6.82V5.507h10.36v1.727M9.41 22.777V13.28h5.182v9.498H9.409M12 15.006a0.864 0.864 0 0 0-0.864 0.863A0.864 0.864 0 0 0 12 16.733a0.864 0.864 0 0 0 0.864-0.864A0.864 0.864 0 0 0 12 15.006z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        addPath(
            pathData = addPathNodes("M9.638 4.86L7.454 2.678M12 4.31V1.224m2.362 3.636l2.184-2.183"),
            fill = SolidColor(Color(0xFFFFFFFF)),
            stroke = SolidColor(Color(0xFFFFFFFF)),
            strokeLineWidth = 1.0f,
        )
        }.build().also { _Strobe = it }
    }

private var _Strobe: ImageVector? = null
