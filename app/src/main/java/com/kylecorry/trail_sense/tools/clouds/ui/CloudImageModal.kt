package com.kylecorry.trail_sense.tools.clouds.ui

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.kylecorry.andromeda.alerts.Alerts
import com.kylecorry.sol.science.meteorology.clouds.CloudGenus
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.tools.clouds.infrastructure.CloudDetailsService

class CloudImageModal(private val context: Context) {

    private val details = CloudDetailsService(context)

    fun show(cloud: CloudGenus?) {
        if (cloud != null) {
            Alerts.image(
                context,
                details.getCloudName(cloud),
                details.getCloudImage(context, cloud)
            )
        }
    }

    private fun Alerts.image(
        context: Context,
        title: CharSequence,
        drawable: Drawable?
    ): AlertDialog {
        val view = ComposeView(context).apply {
            setContent {
                TrailSenseTheme {
                    CloudImage(drawable)
                }
            }
        }

        return dialog(context, title, contentView = view, cancelText = null)
    }

}

@Composable
private fun CloudImage(drawable: Drawable?) {
    val bitmap = drawable?.toBitmap()?.asImageBitmap()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null
            )
        }
    }
}
