package com.kylecorry.andromeda.files

import okio.Path.Companion.toPath
import okio.Source
import platform.Foundation.NSBundle

private fun bundleStorage(): FileStorage = FileStorage(requireNotNull(NSBundle.mainBundle.resourcePath).toPath())
actual fun openAsset(path: String): Source = bundleStorage().source(assetPath(path))
actual fun listAssets(path: String): List<String> = bundleStorage().list(assetPath(path)).map { it.name }
