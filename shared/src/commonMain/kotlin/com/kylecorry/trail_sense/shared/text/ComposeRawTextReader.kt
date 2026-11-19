package com.kylecorry.trail_sense.shared.text

import androidx.compose.ui.text.intl.Locale
import com.kylecorry.trail_sense.shared.generated.resources.Res
import kotlinx.coroutines.CancellationException

/** Reads the app's bundled text, preferring the device's language and region. */
class ComposeRawTextReader : RawTextReader {
    override suspend fun read(text: RawText): String {
        val locale = Locale.current
        val language = if (locale.language == "he") "iw" else locale.language
        val directories = listOfNotNull(
            locale.region.takeIf { it.isNotEmpty() }?.let { "raw-$language-r$it" },
            "raw-$language",
            "raw"
        ).distinct()
        for (directory in directories) {
            for (extension in listOf("txt", "md", "json")) {
                val path = "$directory/${text.name}.$extension"
                if (path !in bundledRawTextPaths) continue
                try {
                    return Res.readBytes("files/trailtext/$path").decodeToString()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Try the next localized file or the default resource.
                }
            }
        }
        error("No bundled text named ${text.name}")
    }
}
