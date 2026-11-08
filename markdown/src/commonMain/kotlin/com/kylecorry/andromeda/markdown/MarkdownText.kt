package com.kylecorry.andromeda.markdown

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl

@Composable
fun MarkdownText(content: String, modifier: Modifier = Modifier) {
    Markdown(content, modifier = modifier, imageTransformer = Coil3ImageTransformerImpl)
}
