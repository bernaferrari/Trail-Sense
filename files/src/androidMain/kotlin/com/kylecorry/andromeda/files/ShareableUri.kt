package com.kylecorry.andromeda.files

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import okio.Path
import java.io.File

/** Creates a content URI using a FileProvider configured by the host application. */
fun shareableUri(context: Context, authority: String, path: Path): Uri =
    FileProvider.getUriForFile(context, authority, File(path.toString()))
