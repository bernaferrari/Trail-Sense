package com.kylecorry.andromeda.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Shows markdown [blocks] (from [Markdown.toBlocks]) as a column: each text run through [text] and each image
 * through [image], [spacing] apart.
 */
@Composable
fun MarkdownContent(
    blocks: List<MarkdownBlock>,
    modifier: Modifier = Modifier,
    spacing: Dp = 16.dp,
    image: @Composable (MarkdownBlock.Image) -> Unit = { MarkdownImage(it) },
    text: @Composable (AnnotatedString) -> Unit = { BasicText(it) }
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Text -> text(block.text)
                is MarkdownBlock.Image -> image(block)
            }
        }
    }
}

/**
 * A markdown image at full width, keeping its aspect ratio. It loads with Coil, so on Android the source can be an
 * asset (`file:///android_asset/…`), a resource (`android.resource://…`), a file (`file://…`) or a content URI.
 */
@Composable
fun MarkdownImage(image: MarkdownBlock.Image, modifier: Modifier = Modifier) {
    AsyncImage(
        model = image.source,
        contentDescription = image.description.ifBlank { image.title },
        contentScale = ContentScale.FillWidth,
        modifier = modifier.fillMaxWidth()
    )
}
