package com.kylecorry.trail_sense.shared.icons.trail

// Generated from res/drawable/photo_maps.xml; redraw freely, it is ours now.
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp

val TrailIcons.PhotoMaps: ImageVector
    get() {
        _PhotoMaps?.let { return it }
        return ImageVector.Builder(
            name = "PhotoMaps",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
            autoMirror = false
        ).apply {
        addPath(
            pathData = addPathNodes("M8.615,2.539 L3.367,4.307C3.172,4.372 3.033,4.54 3.033,4.754L3.033,18.82a0.465,0.465 0,0 0,0.465 0.465c0.047,0 0.102,0.001 0.148,-0.027l4.969,-1.926 1.178,0.412L9.793,15.877L8.615,15.463L8.615,4.4l5.582,1.961v3.926h5.527c0.018,0 0.037,0.001 0.055,0.002L19.779,3.004A0.465,0.465 0,0 0,19.314 2.539h-0.148l-4.969,1.953z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        addPath(
            pathData = addPathNodes("m13.973,16.632 l1.262,1.515 1.767,-2.272 2.272,3.03h-7.07m8.08,0.505v-7.07c0,-0.561 -0.454,-1.01 -1.01,-1.01h-7.07a1.01,1.01 0,0 0,-1.01 1.01v7.07a1.01,1.01 0,0 0,1.01 1.01h7.07a1.01,1.01 0,0 0,1.01 -1.01z"),
            fill = SolidColor(Color(0xFFFF6D00)),
        )
        }.build().also { _PhotoMaps = it }
    }

private var _PhotoMaps: ImageVector? = null
