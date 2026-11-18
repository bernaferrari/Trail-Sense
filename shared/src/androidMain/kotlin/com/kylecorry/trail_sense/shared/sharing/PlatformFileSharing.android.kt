package com.kylecorry.trail_sense.shared.sharing

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

actual fun platformFileUri(file: PlatformFile): String? {
    return runCatching { file.path }.getOrNull()
}

actual suspend fun shareText(title: String, text: String): Boolean {
    return false
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

actual suspend fun copyToClipboard(text: String): Boolean = false
