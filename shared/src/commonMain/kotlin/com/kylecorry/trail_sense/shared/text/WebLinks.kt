package com.kylecorry.trail_sense.shared.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString

private val webUrl = Regex("""(?i)\b(?:https?://|www\.)[^\s<>"]+""")
private const val TRAILING_PUNCTUATION = ".,;:!?'\""

/**
 * The text with each web address (http://, https:// or www.) turned into a link, keeping any styles the text
 * already has. Trailing punctuation and unbalanced closing brackets are left out of the link.
 */
fun CharSequence.withWebLinks(): AnnotatedString {
    val source = this
    return buildAnnotatedString {
        if (source is AnnotatedString) append(source) else append(source.toString())
        webUrl.findAll(source).forEach { match ->
            val start = match.range.first
            val end = linkEnd(source, start, match.range.last + 1)
            if (end <= start) return@forEach
            val text = source.substring(start, end)
            val url = if (text.startsWith("www.", ignoreCase = true)) "https://$text" else text
            addLink(LinkAnnotation.Url(url), start, end)
        }
    }
}

private fun linkEnd(source: CharSequence, start: Int, matchEnd: Int): Int {
    var end = matchEnd
    while (end > start) {
        val last = source[end - 1]
        val trim = when (last) {
            in TRAILING_PUNCTUATION -> true
            ')' -> isUnbalanced(source, start, end, '(', ')')
            ']' -> isUnbalanced(source, start, end, '[', ']')
            else -> false
        }
        if (!trim) break
        end--
    }
    return end
}

private fun isUnbalanced(source: CharSequence, start: Int, end: Int, open: Char, close: Char): Boolean {
    var opened = 0
    var closed = 0
    for (i in start until end) {
        when (source[i]) {
            open -> opened++
            close -> closed++
        }
    }
    return closed > opened
}
