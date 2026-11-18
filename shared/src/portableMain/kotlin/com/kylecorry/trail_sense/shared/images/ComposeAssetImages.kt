package com.kylecorry.trail_sense.shared.images

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.kylecorry.trail_sense.shared.generated.resources.Res
import kotlinx.coroutines.CancellationException
import org.jetbrains.skia.Image

class ComposeAssetImages : AppImages {
    override fun asset(relativePath: String): String = "android-assets://$relativePath"

    override suspend fun load(path: String, maxWidth: Int?, maxHeight: Int?): ImageBitmap? {
        val asset = when {
            path.startsWith("android-assets://") -> path.removePrefix("android-assets://")
            path.startsWith("file:///android_asset/") -> path.removePrefix("file:///android_asset/")
            else -> return null
        }
        if (asset.startsWith("/") || asset.split('/').any { it == ".." }) {
            return null
        }
        return try {
            Image.makeFromEncoded(Res.readBytes("files/trailimages/$asset")).toComposeImageBitmap()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
