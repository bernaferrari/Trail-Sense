package com.kylecorry.trail_sense.shared.text.markdown

import androidx.compose.ui.text.AnnotatedString
import com.kylecorry.andromeda.markdown.Markdown
import com.kylecorry.andromeda.markdown.MarkdownDelimiter
import com.kylecorry.andromeda.markdown.toAnnotatedString

/**
 * Turns the app's markdown (strings, guides, plugin and layer text) into Compose text. Links are
 * [androidx.compose.ui.text.LinkAnnotation]s; screens color them with
 * [com.kylecorry.trail_sense.shared.views.compose.toAnnotatedString].
 */
class MarkdownService(private val delimiters: List<MarkdownDelimiter> = emptyList()) {

    fun toMarkdown(markdown: String): AnnotatedString {
        return Markdown.toAnnotatedString(markdown, delimiters = delimiters)
    }
}
