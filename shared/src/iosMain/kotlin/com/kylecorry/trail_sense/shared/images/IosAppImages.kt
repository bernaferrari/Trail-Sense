package com.kylecorry.trail_sense.shared.images

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.kylecorry.trail_sense.shared.generated.resources.Res
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CancellationException
import org.jetbrains.skia.Image
import platform.Foundation.NSURL

/** iOS [AppImages] implementation decoding bundled images via Compose Resources and Skia. */
class IosAppImages : AppImages {
    override fun asset(relativePath: String): String = "android-assets://$relativePath"

    override suspend fun load(path: String, maxWidth: Int?, maxHeight: Int?): ImageBitmap? {
        val bytes = when {
            path.startsWith("android-assets://") -> {
                val asset = path.removePrefix("android-assets://")
                if (asset.startsWith("/") || asset.split('/').any { it == ".." }) return null
                runCatching { Res.readBytes("files/trailimages/$asset") }.getOrNull()
            }
            path.startsWith("file:///android_asset/") -> {
                val asset = path.removePrefix("file:///android_asset/")
                if (asset.startsWith("/") || asset.split('/').any { it == ".." }) return null
                runCatching { Res.readBytes("files/trailimages/$asset") }.getOrNull()
            }
            path.startsWith("file://") -> {
                val fsPath = NSURL.URLWithString(path)?.path ?: return null
                runCatching { PlatformFile(fsPath).readBytes() }.getOrNull()
            }
            path.startsWith("/") -> {
                runCatching { PlatformFile(path).readBytes() }.getOrNull()
            }
            else -> null
        } ?: return null

        return try {
            Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }
}
