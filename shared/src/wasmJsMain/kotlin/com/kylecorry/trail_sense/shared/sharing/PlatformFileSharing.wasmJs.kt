package com.kylecorry.trail_sense.shared.sharing

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.download
import kotlinx.browser.window

actual fun platformFileUri(file: PlatformFile): String? {
    return null
}

actual suspend fun saveExportFile(
    suggestedName: String,
    extension: String,
    bytes: ByteArray
): Boolean {
    val fileName = if (suggestedName.endsWith(".$extension", ignoreCase = true)) {
        suggestedName
    } else {
        "$suggestedName.$extension"
    }
    FileKit.download(bytes = bytes, fileName = fileName)
    return true
}

actual suspend fun shareText(title: String, text: String): Boolean {
    copyToClipboard(text)
    return true
}

actual suspend fun shareFile(suggestedName: String, bytes: ByteArray): Boolean {
    FileKit.download(bytes = bytes, fileName = suggestedName.ifBlank { "export.dat" })
    return true
}

actual suspend fun openExternalUrl(url: String): Boolean {
    window.open(url, "_blank")
    return true
}

actual suspend fun openEmail(
    recipient: String,
    subject: String,
    body: String
): Boolean {
    val mailto = "mailto:$recipient?subject=${encodeUriComponent(subject)}&body=${encodeUriComponent(body)}"
    window.open(mailto, "_self")
    return true
}

actual fun copyToClipboard(text: String) {
    runCatching {
        window.navigator.clipboard.writeText(text)
    }
}

private fun encodeUriComponent(value: String): String = js("encodeURIComponent(value)")
