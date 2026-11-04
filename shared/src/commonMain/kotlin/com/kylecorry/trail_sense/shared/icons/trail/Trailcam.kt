package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/trailcam.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.Trailcam: ImageVector
    get() {
        _Trailcam?.let { return it }
        return ImageVector.Builder(
            name = "Trailcam",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M6.754,1.532C6.2,1.532 5.754,1.978 5.754,2.532L5.754,4.435C5.445,4.605 5.237,4.933 5.237,5.311L5.237,18.689C5.237,19.067 5.445,19.395 5.754,19.565L5.754,21.468C5.754,22.022 6.2,22.468 6.754,22.468L17.246,22.468C17.8,22.468 18.246,22.022 18.246,21.468L18.246,19.565C18.555,19.395 18.762,19.067 18.762,18.689L18.762,5.311C18.762,4.933 18.555,4.605 18.246,4.435L18.246,2.532C18.246,1.978 17.8,1.532 17.246,1.532L6.754,1.532zM12,4.844A2.806,2.806 0,0 1,14.805 7.649A2.806,2.806 0,0 1,12 10.455A2.806,2.806 0,0 1,9.194 7.649A2.806,2.806 0,0 1,12 4.844zM10.303,12.466L13.697,12.466C13.974,12.466 14.197,12.689 14.197,12.966L14.197,14.798C14.197,15.075 13.974,15.298 13.697,15.298L10.303,15.298C10.026,15.298 9.803,15.075 9.803,14.798L9.803,12.966C9.803,12.689 10.026,12.466 10.303,12.466z"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        addPath(
            pathData = addPathNodes("M12,7.723m-1.977,0a1.977,1.977 0,1 1,3.953 0a1.977,1.977 0,1 1,-3.953 0"),
            fill = SolidColor(Color(0xFFFFFFFF)),
        )
        }.build().also { _Trailcam = it }
    }

private var _Trailcam: ImageVector? = null
