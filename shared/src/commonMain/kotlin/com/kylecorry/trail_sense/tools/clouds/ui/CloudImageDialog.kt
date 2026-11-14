package com.kylecorry.trail_sense.tools.clouds.ui

import com.kylecorry.trail_sense.shared.text.LocalAppStrings
import com.kylecorry.trail_sense.shared.text.SystemString
import com.kylecorry.trail_sense.shared.text.systemString

import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.images.rememberAppImage
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsText
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseDialog
import androidx.compose.ui.draw.clip

/** A reference photo of a cloud type, edge to edge inside the dialog so the picture is the content. */
@Composable
fun CloudImageDialog(
    cloud: CloudGenus,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val details = remember(strings) { CloudDetailsText(strings) }
    val title = details.getCloudName(cloud)
    val bitmap = rememberAppImage(details.getCloudImagePath(cloud))

    TrailSenseDialog(
        onDismissRequest = onDismiss,
        title = title,
        confirmText = systemString(SystemString.Ok)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.coerceAtLeast(1))
                    .clip(MaterialTheme.shapes.large)
            )
        }
    }
}
