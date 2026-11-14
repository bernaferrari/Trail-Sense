package com.kylecorry.andromeda.files

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile

/** Creates an Android document with an explicit MIME type in a selected document tree. */
public fun createDocument(context: Context, directory: PlatformFile, name: String, mimeType: String): PlatformFile? {
    val uri = (directory.androidFile as? AndroidFile.UriWrapper)?.uri ?: return null
    val tree = DocumentFile.fromTreeUri(context, uri) ?: return null
    return tree.createFile(mimeType, name)?.uri?.let { PlatformFile(it) }
}
