package com.kylecorry.trail_sense.shared.sharing

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

actual fun platformFileUri(file: PlatformFile): String? = file.path

actual suspend fun shareText(title: String, text: String): Boolean = copyText(text)

actual suspend fun shareFile(suggestedName: String, bytes: ByteArray): Boolean {
    val name = suggestedName.substringBeforeLast('.').ifBlank { "export" }
    val extension = suggestedName.substringAfterLast('.', "dat")
    return saveExportFile(name, extension, bytes)
}

actual suspend fun openExternalUrl(url: String): Boolean = runCatching {
    Desktop.getDesktop().browse(URI(url))
}.isSuccess

actual suspend fun openEmail(recipient: String, subject: String, body: String): Boolean = runCatching {
    Desktop.getDesktop().mail(URI("mailto:$recipient?subject=${subject.urlComponent()}&body=${body.urlComponent()}"))
}.isSuccess

actual suspend fun copyToClipboard(text: String): Boolean = copyText(text)

private fun copyText(text: String): Boolean = runCatching {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}.isSuccess

private fun String.urlComponent(): String =
    URLEncoder.encode(this, StandardCharsets.UTF_8).replace("+", "%20")
