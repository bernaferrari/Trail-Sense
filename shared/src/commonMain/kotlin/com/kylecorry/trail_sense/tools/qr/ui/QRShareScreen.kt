package com.kylecorry.trail_sense.tools.qr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kylecorry.andromeda.bitmaps.decodeImage
import com.kylecorry.andromeda.qr.QRCodeGenerator
import com.kylecorry.trail_sense.shared.generated.resources.Res
import com.kylecorry.trail_sense.shared.generated.resources.qr_code
import com.kylecorry.trail_sense.shared.generated.resources.qr_text_too_long
import com.kylecorry.trail_sense.shared.text.appString
import com.kylecorry.trail_sense.shared.views.compose.AppHost
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseInstrumentScaffold
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseShape
import com.kylecorry.trail_sense.shared.views.compose.TrailSenseSpacing
import kotlin.math.floor
import kotlin.math.min

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QRShareScreen(title: String, text: String, modifier: Modifier = Modifier) {
    val tooLongMessage = appString(Res.string.qr_text_too_long)
    LaunchedEffect(text) {
        if (text.length > QR_SHARE_MAX_LENGTH) AppHost.current().snackbar(tooLongMessage)
    }
    val qr = remember(text) {
        decodeImage(QRCodeGenerator.png(text.take(QR_SHARE_MAX_LENGTH), pixelsPerModule = 1))
    }
    val description = appString(Res.string.qr_code)
    TrailSenseInstrumentScaffold(title = title, modifier = modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(TrailSenseSpacing.lg), contentAlignment = Alignment.Center) {
            Box(Modifier.widthIn(max = 420.dp).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = MaterialShapes.Cookie12Sided.toShape(),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {}
                Canvas(
                    Modifier.fillMaxSize(0.66f)
                        .clip(RoundedCornerShape(TrailSenseShape.groupOuter))
                        .background(Color.White)
                        .padding(TrailSenseSpacing.lg)
                        .semantics { contentDescription = description }
                ) {
                    // Whole physical pixels keep module edges sharp for scanners.
                    val available = min(size.width / qr.width, size.height / qr.height)
                    val module = if (available >= 1f) floor(available) else available
                    val left = floor((size.width - qr.width * module) / 2f)
                    val top = floor((size.height - qr.height * module) / 2f)
                    for (y in 0 until qr.height) {
                        var x = 0
                        while (x < qr.width) {
                            if ((qr[x, y] and 0x00ffffff) != 0) {
                                x++
                                continue
                            }
                            val start = x
                            while (x < qr.width && (qr[x, y] and 0x00ffffff) == 0) x++
                            drawRect(Color.Black, Offset(left + start * module, top + y * module), Size((x - start) * module, module))
                        }
                    }
                }
            }
        }
    }
}
