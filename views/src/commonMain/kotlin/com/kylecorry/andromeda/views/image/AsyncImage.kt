package com.kylecorry.andromeda.views.image

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage as CoilAsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
public fun AsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    CoilAsyncImage(model, contentDescription, modifier, contentScale = contentScale)
}

@Composable
public fun AsyncImage(
    key: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    load: suspend () -> ImageBitmap,
) {
    val bitmap by produceState<ImageBitmap?>(null, key) {
        value = withContext(Dispatchers.Default) { load() }
    }
    bitmap?.let { Image(it, contentDescription, modifier, contentScale = contentScale) }
}
