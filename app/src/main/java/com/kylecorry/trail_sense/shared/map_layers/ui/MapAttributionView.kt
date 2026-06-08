package com.kylecorry.trail_sense.shared.map_layers.ui

import android.content.Context
import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kylecorry.trail_sense.shared.views.TrailSenseTheme

private const val URL_ANNOTATION_TAG = "url"

class MapAttributionView(context: Context) : AbstractComposeView(context) {

    var text by mutableStateOf<CharSequence?>(null)

    @Suppress("DEPRECATION")
    @Composable
    override fun Content() {
        val attribution = text ?: return
        val uriHandler = LocalUriHandler.current
        val annotated = remember(attribution) {
            buildAnnotatedString {
                append(attribution.toString())
                if (attribution is Spanned) {
                    attribution.getSpans(0, attribution.length, URLSpan::class.java)
                        .forEach { span ->
                            val start = attribution.getSpanStart(span)
                            val end = attribution.getSpanEnd(span)
                            if (start >= 0 && end > start) {
                                addStringAnnotation(URL_ANNOTATION_TAG, span.url, start, end)
                                addStyle(
                                    SpanStyle(
                                        color = Color.White,
                                        textDecoration = TextDecoration.Underline
                                    ),
                                    start,
                                    end
                                )
                            }
                        }
                }
            }
        }

        TrailSenseTheme {
            Surface(color = Color(0x88000000)) {
                ClickableText(
                    text = annotated,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontSize = 8.sp
                    ),
                    onClick = { offset ->
                        annotated
                            .getStringAnnotations(URL_ANNOTATION_TAG, offset, offset)
                            .firstOrNull()
                            ?.let { uriHandler.openUri(it.item) }
                    }
                )
            }
        }
    }
}
