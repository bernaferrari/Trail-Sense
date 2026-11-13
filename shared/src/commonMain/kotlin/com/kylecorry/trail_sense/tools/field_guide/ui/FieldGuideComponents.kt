@file:Suppress("FunctionNaming")
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.kylecorry.trail_sense.tools.field_guide.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSize

/** A page photo in the icon well's footprint; the well holds its place while the photo loads. */
@Composable
fun FieldGuideThumbnail(
    bitmap: ImageBitmap?,
    fallback: Painter,
    modifier: Modifier = Modifier
) {
    if (bitmap == null) {
        com.kylecorry.trail_sense.shared.views.compose.TrailSenseRowWell(icon = fallback, modifier = modifier)
        return
    }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(TrailSenseSize.compactIconContainer)
            .clip(RoundedCornerShape(12.dp))
    )
}

/** A full-bleed photo inside a hero or group card. */
@Composable
fun FieldGuideBitmapImage(
    bitmap: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
