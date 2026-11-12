package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration

/**
 * Text (such as rendered markdown) ready to draw: its links take [linkColor] and an underline, and open when
 * [linksClickable] (Compose's Text opens [LinkAnnotation.Url]s itself); otherwise they only keep their look.
 */
fun CharSequence.toAnnotatedString(linkColor: Color, linksClickable: Boolean = true): AnnotatedString {
    if (this !is AnnotatedString) return AnnotatedString(toString())
    val links = getLinkAnnotations(0, length)
    if (links.isEmpty()) return this
    val linkStyle = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
    return buildAnnotatedString {
        // Rebuilt from the styles alone so the links can be replaced.
        append(AnnotatedString(text, spanStyles, paragraphStyles))
        links.forEach { range ->
            val url = (range.item as? LinkAnnotation.Url)?.url
            if (linksClickable && url != null) {
                addLink(LinkAnnotation.Url(url, TextLinkStyles(linkStyle)), range.start, range.end)
            } else {
                addStyle(linkStyle, range.start, range.end)
            }
        }
    }
}
