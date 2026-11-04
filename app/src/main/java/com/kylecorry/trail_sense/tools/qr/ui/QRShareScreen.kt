package com.kylecorry.trail_sense.tools.qr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.trail_sense.shared.dialogs.Alerts
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing

@Composable
fun QRShareScreen(
    title: String,
    text: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(text) {
        if (text.length > QR_SHARE_MAX_LENGTH) {
            Alerts.toast(context, context.getString(R.string.qr_text_too_long))
        }
    }

    QRShareContent(
        title = title,
        text = text.take(QR_SHARE_MAX_LENGTH),
        modifier = modifier.fillMaxSize()
    )
}

const val QR_SHARE_MAX_LENGTH = 1000

@Composable
private fun QRShareContent(
    title: String,
    text: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val qrBitmap = remember(context, text) {
        val size = Resources.dp(context, 250f).toInt()
        QR.encode(text, size, size).asImageBitmap()
    }

    TrailSenseInstrumentScaffold(
        title = title,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(TrailSenseSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            // The code is the whole screen. Its immediate background stays white so it scans in any theme.
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(TrailSenseShape.groupOuter))
                    .background(Color.White)
                    .padding(TrailSenseSpacing.xl),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qrBitmap,
                    contentDescription = stringResource(R.string.qr_code),
                    modifier = Modifier.size(250.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun QRShareContentPreview() {
    TrailSenseTheme {
        QRShareContent(
            title = "QR Code",
            text = "Trail Sense"
        )
    }
}
