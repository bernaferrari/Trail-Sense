package com.kylecorry.trail_sense.shared.sharing

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.write

actual suspend fun saveExportFile(suggestedName: String, extension: String, bytes: ByteArray): Boolean {
    val file = FileKit.openFileSaver(suggestedName = suggestedName, extension = extension) ?: return false
    file.write(bytes)
    return true
}
