package com.kylecorry.trail_sense.shared.sharing

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes

data class PickedFile(
    val name: String,
    val bytes: ByteArray,
    val path: String? = null
) {
    fun decodeToString(): String = bytes.decodeToString()
}

suspend fun pickFile(extensions: List<String>? = null): PickedFile? {
    val type = if (extensions.isNullOrEmpty()) {
        FileKitType.File()
    } else {
        FileKitType.File(extensions.toSet())
    }
    val file = FileKit.openFilePicker(type = type) ?: return null
    return file.toPickedFile()
}

suspend fun pickImageFile(): PickedFile? {
    val file = FileKit.openFilePicker(type = FileKitType.Image) ?: return null
    return file.toPickedFile()
}

private suspend fun PlatformFile.toPickedFile(): PickedFile {
    return PickedFile(
        name = name,
        bytes = readBytes(),
        path = platformFileUri(this)
    )
}

expect fun platformFileUri(file: PlatformFile): String?

expect suspend fun saveExportFile(
    suggestedName: String,
    extension: String,
    bytes: ByteArray
): Boolean

expect suspend fun shareText(title: String, text: String): Boolean

expect suspend fun shareFile(suggestedName: String, bytes: ByteArray): Boolean

expect suspend fun openExternalUrl(url: String): Boolean

expect suspend fun openEmail(
    recipient: String,
    subject: String,
    body: String
): Boolean

expect fun copyToClipboard(text: String)
