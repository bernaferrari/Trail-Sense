package com.kylecorry.andromeda.files

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.context
import okio.Source
import okio.source

public actual fun openAsset(path: String): Source = FileKit.context.assets.open(assetPath(path)).source()
public actual fun listAssets(path: String): List<String> = FileKit.context.assets.list(assetPath(path))?.toList().orEmpty()
