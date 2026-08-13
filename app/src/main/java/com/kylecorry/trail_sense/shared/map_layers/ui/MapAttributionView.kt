package com.kylecorry.trail_sense.shared.map_layers.ui

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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

private const val URL_ANNOTATION_TAG = "url"

class MapAttributionState {

    var text by mutableStateOf<CharSequence?>(null)
    var isVisible by mutableStateOf(false)

    @Suppress("DEPRECATION")
    @Composable
    fun Content(modifier: Modifier = Modifier) {
        if (!isVisible) {
            return
        }
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

        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            color = Color(0xB8000000)
        ) {
            ClickableText(
                text = annotated,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White
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
