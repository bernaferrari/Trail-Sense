package com.kylecorry.trail_sense.shared.sharing

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.dialogs.shareFile
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.write

actual fun platformFileUri(file: PlatformFile): String? {
    return runCatching { file.path }.getOrNull()
}

actual suspend fun saveExportFile(
    suggestedName: String,
    extension: String,
    bytes: ByteArray
): Boolean {
    val target = FileKit.openFileSaver(
        suggestedName = suggestedName,
        extension = extension
    ) ?: return false
    target.write(bytes)
    return true
}

actual suspend fun shareText(title: String, text: String): Boolean {
    return false
}

actual suspend fun shareFile(suggestedName: String, bytes: ByteArray): Boolean {
    val sanitizedName = suggestedName.substringAfterLast('/').ifBlank { "export.dat" }
    val tempFile = PlatformFile(FileKit.cacheDir, sanitizedName)
    tempFile.write(bytes)
    FileKit.shareFile(tempFile)
    return true
}

actual suspend fun openExternalUrl(url: String): Boolean {
    return false
}

actual suspend fun openEmail(
    recipient: String,
    subject: String,
    body: String
): Boolean {
    return false
}

actual fun copyToClipboard(text: String) {
    // Android uses TrailSenseComposeDestinationHost + Clipboard directly in :app.
}
