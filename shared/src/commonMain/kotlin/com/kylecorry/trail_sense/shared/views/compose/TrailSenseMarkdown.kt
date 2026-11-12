package com.kylecorry.trail_sense.shared.views.compose

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.kylecorry.andromeda.core.cache.DependencyRegistry
import com.kylecorry.andromeda.markdown.MarkdownContent
import com.kylecorry.trail_sense.shared.text.markdown.MarkdownService

/**
 * The app's [markdown] with its images: text runs as [TrailSenseLinkedText] and images (assets, resources or files)
 * at full width between them.
 */
@Composable
fun TrailSenseMarkdown(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    textAlign: TextAlign? = null,
    service: MarkdownService = remember { DependencyRegistry.get<MarkdownService>() }
) {
    val blocks = remember(markdown, service) { service.toBlocks(markdown) }
    MarkdownContent(blocks, modifier = modifier, spacing = TrailSenseSpacing.md) { text ->
        TrailSenseLinkedText(text = text, style = style, textAlign = textAlign)
    }
}
