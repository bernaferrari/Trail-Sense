package com.kylecorry.trail_sense.shared.views.compose

import android.text.Spanned
import android.text.style.URLSpan
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration

private const val UrlAnnotationTag = "url"

@Composable
fun TrailSenseLinkedText(
    text: CharSequence,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    textAlign: TextAlign? = null
) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val displayStyle = if (textAlign == null) {
        style.copy(color = textColor)
    } else {
        style.copy(color = textColor, textAlign = textAlign)
    }
    val annotated = remember(text, linkColor) {
        buildAnnotatedString {
            append(text.toString())
            if (text is Spanned) {
                text.getSpans(0, text.length, URLSpan::class.java).forEach { span ->
                    val start = text.getSpanStart(span)
                    val end = text.getSpanEnd(span)
                    if (start >= 0 && end > start) {
                        addStringAnnotation(UrlAnnotationTag, span.url, start, end)
                        addStyle(
                            SpanStyle(
                                color = linkColor,
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

    ClickableText(
        text = annotated,
        modifier = modifier,
        style = displayStyle,
        onClick = { offset ->
            annotated
                .getStringAnnotations(UrlAnnotationTag, offset, offset)
                .firstOrNull()
                ?.let { uriHandler.openUri(it.item) }
        }
    )
}
