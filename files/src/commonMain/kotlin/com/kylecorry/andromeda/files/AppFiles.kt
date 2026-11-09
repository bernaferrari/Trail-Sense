package com.kylecorry.andromeda.files

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import okio.Path.Companion.toPath

/** Persistent application files. Initialize FileKit before calling on platforms that require it. */
fun appFiles(): FileStorage = FileStorage(FileKit.filesDir.path.toPath())

/** Application cache files. The operating system may remove them at any time. */
fun appCache(): FileStorage = FileStorage(FileKit.cacheDir.path.toPath())
