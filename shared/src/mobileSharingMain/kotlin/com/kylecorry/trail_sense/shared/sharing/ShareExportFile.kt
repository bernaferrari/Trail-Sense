package com.kylecorry.trail_sense.shared.sharing

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.dialogs.shareFile
import io.github.vinceglb.filekit.write

actual suspend fun shareFile(suggestedName: String, bytes: ByteArray): Boolean {
    val name = suggestedName.substringAfterLast('/').substringAfterLast('\\').takeUnless { it.isBlank() || it == "." || it == ".." } ?: "export.dat"
    val file = PlatformFile(FileKit.cacheDir, name)
    file.write(bytes)
    FileKit.shareFile(file)
    return true
}
