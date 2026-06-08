package com.kylecorry.trail_sense.tools.qr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.alerts.toast
import com.kylecorry.andromeda.core.system.Resources
import com.kylecorry.andromeda.qr.QR
import com.kylecorry.trail_sense.R
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseBottomSheetComposeFragment

class ViewQRBottomSheet(
    private val title: String,
    private val text: String
) : TrailSenseBottomSheetComposeFragment() {

    @Composable
    override fun FragmentContent() {
        LaunchedEffect(text) {
            if (text.length > MAX_LENGTH) {
                toast(getString(R.string.qr_text_too_long))
            }
        }

        QRShareContent(
            title = title,
            text = text.take(MAX_LENGTH),
            modifier = Modifier.fillMaxSize()
        )
    }

    companion object {
        const val MAX_LENGTH = 1000
    }
}

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

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Image(
            bitmap = qrBitmap,
            contentDescription = stringResource(R.string.qr_code),
            modifier = Modifier
                .padding(top = 16.dp, bottom = 16.dp)
                .size(250.dp)
                .shadow(4.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
        )
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
