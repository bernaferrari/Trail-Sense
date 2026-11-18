package com.kylecorry.trail_sense.shared.images

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.kylecorry.trail_sense.shared.generated.resources.Res
import kotlinx.coroutines.CancellationException
import org.jetbrains.skia.Image

class ComposeBundledImages : BundledImages {
    override suspend fun load(path: String): ImageBitmap? {
        val asset = path.removePrefix("file:///android_asset/")
        if (asset.startsWith("/") || asset.split('/').any { it == ".." }) return null
        return try {
            Image.makeFromEncoded(Res.readBytes("files/trailimages/$asset")).toComposeImageBitmap()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
